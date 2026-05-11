package kz.projem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.projem.domain.enums.ReminderStatus;
import kz.projem.domain.enums.TaskPriority;
import kz.projem.domain.enums.TaskStatus;
import kz.projem.domain.model.*;
import kz.projem.dto.request.ChatRequest;
import kz.projem.dto.response.ChatResponse;
import kz.projem.exception.AiLimitExceededException;
import kz.projem.exception.ResourceNotFoundException;
import kz.projem.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final UserService userService;
    private final WebClient.Builder webClientBuilder;
    private final TaskRepository taskRepository;
    private final NoteRepository noteRepository;
    private final ReminderRepository reminderRepository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    @Value("${app.ai.api-key}")
    private String apiKey;

    @Value("${app.ai.base-url}")
    private String baseUrl;

    @Value("${app.ai.model}")
    private String model;

    @Value("${app.ai.max-tokens}")
    private int maxTokens;

    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("MMM d, HH:mm");
    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("MMM d");

    // ─── Tool definitions (OpenAI function-calling format) ────────────────────

    private static final List<Map<String, Object>> TOOLS = buildTools();

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> buildTools() {
        Map<String, Object> priorityEnum = new HashMap<>();
        priorityEnum.put("type", "string");
        priorityEnum.put("enum", List.of("LOW", "MEDIUM", "HIGH", "CRITICAL"));
        priorityEnum.put("description", "Task priority level");

        Map<String, Object> taskProps = new HashMap<>();
        taskProps.put("title",       Map.of("type", "string", "description", "Task title"));
        taskProps.put("priority",    priorityEnum);
        taskProps.put("dueDate",     Map.of("type", "string", "description", "Due date YYYY-MM-DD"));
        taskProps.put("description", Map.of("type", "string", "description", "Optional details"));

        Map<String, Object> reminderProps = new HashMap<>();
        reminderProps.put("title",    Map.of("type", "string", "description", "Reminder title"));
        reminderProps.put("remindAt", Map.of("type", "string", "description", "ISO-8601 datetime, e.g. 2026-05-12T08:00:00"));
        reminderProps.put("message",  Map.of("type", "string", "description", "Optional additional details"));

        Map<String, Object> noteProps = new HashMap<>();
        noteProps.put("title",   Map.of("type", "string", "description", "Note title"));
        noteProps.put("content", Map.of("type", "string", "description", "Note body text"));
        noteProps.put("pinned",  Map.of("type", "boolean", "description", "Pin to top"));

        return List.of(
            tool("create_task",
                "Create a new task/todo item for the user. Use when user says things like 'add task', 'remind me to do', 'I need to', 'create a todo'.",
                taskProps, List.of("title")),
            tool("create_reminder",
                "Set a reminder at a specific date and time. Use when user mentions a time/date and wants to be reminded.",
                reminderProps, List.of("title", "remindAt")),
            tool("create_note",
                "Save a note or important information for the user.",
                noteProps, List.of("title", "content"))
        );
    }

    private static Map<String, Object> tool(String name, String desc,
                                            Map<String, Object> props,
                                            List<String> required) {
        Map<String, Object> params = new HashMap<>();
        params.put("type", "object");
        params.put("properties", props);
        params.put("required", required);

        Map<String, Object> func = new HashMap<>();
        func.put("name", name);
        func.put("description", desc);
        func.put("parameters", params);

        return Map.of("type", "function", "function", func);
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    @Transactional
    public ChatResponse chat(ChatRequest request, User user) {
        if (!user.canMakeAiRequest()) {
            throw new AiLimitExceededException(
                "Monthly AI limit reached (" + user.getAiRequestsLimit() + "). Upgrade your plan.");
        }

        ChatSession session = resolveSession(request.getSessionId(), user, request.getMessage());

        saveMessage("user", request.getMessage(), 0, session);

        List<Map<String, Object>> messages = buildMessages(session, request.getMessage(), user);

        CallResult result = callWithTools(messages, user);

        ChatMessage assistantMsg = saveMessage("assistant", result.reply(), result.tokens(), session);
        userService.incrementAiUsage(user);

        return ChatResponse.builder()
                .sessionId(session.getId())
                .sessionTitle(session.getTitle())
                .messageId(assistantMsg.getId())
                .reply(result.reply())
                .actionsPerformed(result.actions())
                .tokensUsed(result.tokens())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public Page<ChatSession> getSessions(User user, Pageable pageable) {
        return sessionRepository.findByUserId(user.getId(), pageable);
    }

    public List<ChatMessage> getMessages(Long sessionId, User user) {
        sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Chat session", sessionId));
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional
    public void deleteSession(Long sessionId, User user) {
        ChatSession s = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Chat session", sessionId));
        sessionRepository.delete(s);
    }

    // ─── Core AI call with tool execution loop ───────────────────────────────

    private record CallResult(String reply, List<String> actions, int tokens) {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    private CallResult callWithTools(List<Map<String, Object>> messages, User user) {
        if (apiKey == null || apiKey.isBlank() || "none".equalsIgnoreCase(apiKey)) {
            String userMsg = ((String) messages.get(messages.size() - 1).get("content")).toLowerCase();
            return new CallResult(buildDemoReply(messages, userMsg), List.of(), 0);
        }

        List<String> actions = new ArrayList<>();
        int totalTokens = 0;
        String finalReply = "";

        try {
            WebClient client = webClientBuilder.baseUrl(baseUrl).build();

            for (int iter = 0; iter < 4; iter++) {
                Map<String, Object> body = new HashMap<>();
                body.put("model", model);
                body.put("messages", messages);
                body.put("max_tokens", maxTokens);
                body.put("tools", TOOLS);
                body.put("tool_choice", "auto");

                Map response = client.post()
                        .uri("/chat/completions")
                        .header("Authorization", "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                if (response == null) break;

                Map<String, Object> usage = (Map<String, Object>) response.get("usage");
                if (usage != null && usage.get("total_tokens") instanceof Integer t) totalTokens += t;

                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                Map<String, Object> choiceMsg    = (Map<String, Object>) choices.get(0).get("message");
                String finishReason              = (String) choices.get(0).get("finish_reason");

                messages.add(choiceMsg);

                if ("tool_calls".equals(finishReason)) {
                    List<Map<String, Object>> toolCalls =
                            (List<Map<String, Object>>) choiceMsg.get("tool_calls");

                    for (Map<String, Object> tc : toolCalls) {
                        String callId   = (String) tc.get("id");
                        Map<String, Object> fn = (Map<String, Object>) tc.get("function");
                        String fnName   = (String) fn.get("name");
                        String argsJson = (String) fn.get("arguments");

                        String toolResult = executeTool(fnName, argsJson, user, actions);

                        Map<String, Object> toolMsg = new HashMap<>();
                        toolMsg.put("role", "tool");
                        toolMsg.put("tool_call_id", callId);
                        toolMsg.put("content", toolResult);
                        messages.add(toolMsg);
                    }
                } else {
                    finalReply = (String) choiceMsg.get("content");
                    break;
                }
            }
        } catch (Exception e) {
            log.error("AI API call failed: {}", e.getMessage());
            finalReply = "Не смог подключиться к AI-сервису. Проверь API ключ.";
        }

        return new CallResult(finalReply, actions, totalTokens);
    }

    // ─── Tool execution ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String executeTool(String name, String argsJson, User user, List<String> actions) {
        try {
            Map<String, Object> args = objectMapper.readValue(argsJson, Map.class);
            return switch (name) {
                case "create_task"     -> execCreateTask(args, user, actions);
                case "create_reminder" -> execCreateReminder(args, user, actions);
                case "create_note"     -> execCreateNote(args, user, actions);
                default -> "Unknown tool: " + name;
            };
        } catch (Exception e) {
            log.error("Tool '{}' failed: {}", name, e.getMessage());
            return "Error in " + name + ": " + e.getMessage();
        }
    }

    @Transactional
    String execCreateTask(Map<String, Object> args, User user, List<String> actions) {
        String title       = (String) args.get("title");
        String priorityStr = (String) args.getOrDefault("priority", "MEDIUM");
        String dueDateStr  = (String) args.get("dueDate");
        String desc        = (String) args.get("description");

        TaskPriority priority;
        try { priority = TaskPriority.valueOf(priorityStr.toUpperCase()); }
        catch (Exception e) { priority = TaskPriority.MEDIUM; }

        LocalDate dueDate = null;
        if (dueDateStr != null && !dueDateStr.isBlank()) {
            try { dueDate = LocalDate.parse(dueDateStr); } catch (DateTimeParseException ignored) {}
        }

        Task task = Task.builder()
                .title(title).description(desc)
                .priority(priority).status(TaskStatus.TODO)
                .dueDate(dueDate).user(user)
                .build();
        taskRepository.save(task);
        auditService.log(user.getId(), user.getEmail(), "AI_CREATE_TASK", "TASK", task.getId(), title);

        String msg = "Задача создана: «" + title + "» [" + priority + "]"
                + (dueDate != null ? ", срок " + dueDate.format(DATE_FMT) : "");
        actions.add("✅ " + msg);
        return "OK. " + msg;
    }

    @Transactional
    String execCreateReminder(Map<String, Object> args, User user, List<String> actions) {
        String title      = (String) args.get("title");
        String remindAtStr = (String) args.get("remindAt");
        String message    = (String) args.get("message");

        LocalDateTime remindAt;
        try {
            remindAt = LocalDateTime.parse(remindAtStr);
        } catch (DateTimeParseException e) {
            return "Ошибка: неверный формат даты/времени. Нужен ISO-8601 (например 2026-05-12T08:00:00)";
        }

        Reminder reminder = Reminder.builder()
                .title(title).message(message)
                .remindAt(remindAt).status(ReminderStatus.PENDING)
                .user(user).build();
        reminderRepository.save(reminder);
        auditService.log(user.getId(), user.getEmail(), "AI_CREATE_REMINDER", "REMINDER", reminder.getId(), title);

        String msg = "Напоминание: «" + title + "» в " + remindAt.format(DT_FMT);
        actions.add("⏰ " + msg);
        return "OK. " + msg;
    }

    @Transactional
    String execCreateNote(Map<String, Object> args, User user, List<String> actions) {
        String title   = (String) args.get("title");
        String content = (String) args.get("content");
        boolean pinned = Boolean.TRUE.equals(args.get("pinned"));

        Note note = Note.builder()
                .title(title).content(content)
                .pinned(pinned).archived(false)
                .user(user).build();
        noteRepository.save(note);
        auditService.log(user.getId(), user.getEmail(), "AI_CREATE_NOTE", "NOTE", note.getId(), title);

        String msg = "Заметка сохранена: «" + title + "»" + (pinned ? " (закреплена)" : "");
        actions.add("📝 " + msg);
        return "OK. " + msg;
    }

    // ─── Context-aware system prompt ──────────────────────────────────────────

    private String buildSystemPrompt(User user) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ты персональный ассистент пользователя ").append(user.getFirstName()).append(". ");
        sb.append("Сегодня ").append(LocalDate.now()).append(". ");
        sb.append("Отвечай на том языке, на котором пишет пользователь. Будь кратким и конкретным.\n\n");
        sb.append("Ты можешь создавать задачи, напоминания и заметки — для этого используй доступные инструменты (tools).\n\n");

        List<Task> activeTasks = taskRepository.findActiveTasks(user.getId(), PageRequest.of(0, 20));
        sb.append("### АКТИВНЫЕ ЗАДАЧИ (").append(activeTasks.size()).append("):\n");
        if (activeTasks.isEmpty()) {
            sb.append("— нет активных задач\n");
        } else {
            for (Task t : activeTasks) {
                sb.append("- [").append(t.getPriority()).append("] ").append(t.getTitle());
                if ("IN_PROGRESS".equals(t.getStatus().name())) sb.append(" *(в работе)*");
                if (t.getDueDate() != null) sb.append(", срок ").append(t.getDueDate().format(DATE_FMT));
                sb.append("\n");
            }
        }

        List<Reminder> reminders = reminderRepository
                .findByUserIdAndStatus(user.getId(), ReminderStatus.PENDING, PageRequest.of(0, 10))
                .getContent();
        sb.append("\n### НАПОМИНАНИЯ (").append(reminders.size()).append("):\n");
        if (reminders.isEmpty()) {
            sb.append("— нет активных напоминаний\n");
        } else {
            for (Reminder r : reminders) {
                sb.append("- ").append(r.getTitle()).append(" @ ").append(r.getRemindAt().format(DT_FMT));
                if (r.getMessage() != null && !r.getMessage().isBlank())
                    sb.append(" — ").append(truncate(r.getMessage(), 80));
                sb.append("\n");
            }
        }

        List<Note> notes = noteRepository
                .findTop10ByUserIdAndArchivedFalseOrderByPinnedDescCreatedAtDesc(user.getId());
        sb.append("\n### ЗАМЕТКИ (").append(notes.size()).append("):\n");
        if (notes.isEmpty()) {
            sb.append("— нет заметок\n");
        } else {
            for (Note n : notes) {
                sb.append("- ").append(n.isPinned() ? "[📌] " : "").append("**").append(n.getTitle()).append("**: ");
                sb.append(truncate(n.getContent(), 120)).append("\n");
            }
        }

        return sb.toString();
    }

    private List<Map<String, Object>> buildMessages(ChatSession session, String currentMessage, User user) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemPrompt(user)));

        List<ChatMessage> history = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        int start = Math.max(0, history.size() - 10);
        for (int i = start; i < history.size(); i++) {
            ChatMessage m = history.get(i);
            messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }
        messages.add(Map.of("role", "user", "content", currentMessage));
        return messages;
    }

    // ─── Demo mode (no API key) ───────────────────────────────────────────────

    private String buildDemoReply(List<Map<String, Object>> messages, String userMsg) {
        String system = (String) messages.get(0).get("content");

        if (userMsg.contains("task") || userMsg.contains("задач") || userMsg.contains("todo")) {
            long cnt = system.lines().filter(l -> l.startsWith("- [")).count();
            return "📋 *Demo режим.* У тебя **" + cnt + " активных задач**. "
                 + "Добавь API ключ в `application.yml` чтобы включить реальный AI.";
        }
        if (userMsg.contains("remind") || userMsg.contains("напомин")) {
            return "⏰ *Demo режим.* Вижу твои напоминания. "
                 + "Добавь API ключ для работы с реальным AI.";
        }
        if (userMsg.contains("note") || userMsg.contains("заметк")) {
            return "📝 *Demo режим.* Твои заметки загружены. "
                 + "Добавь API ключ для работы с реальным AI.";
        }
        return "👋 *Demo режим* — вижу твои данные, но для умных ответов нужен AI провайдер.\n\n"
             + "**Подключить AI:**\n"
             + "• Groq (бесплатно): ключ с console.groq.com → `app.ai.api-key`\n"
             + "• Ollama (локально): `app.ai.base-url: http://localhost:11434/v1`, `api-key: none`\n"
             + "• LM Studio: `app.ai.base-url: http://localhost:1234/v1`, `api-key: none`";
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ChatMessage saveMessage(String role, String content, int tokens, ChatSession session) {
        ChatMessage msg = ChatMessage.builder()
                .role(role).content(content).tokensUsed(tokens).session(session).build();
        return messageRepository.save(msg);
    }

    private ChatSession resolveSession(Long sessionId, User user, String firstMessage) {
        if (sessionId != null) {
            return sessionRepository.findByIdAndUserId(sessionId, user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chat session", sessionId));
        }
        String title = firstMessage.length() > 50 ? firstMessage.substring(0, 47) + "..." : firstMessage;
        return sessionRepository.save(ChatSession.builder().title(title).user(user).build());
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
