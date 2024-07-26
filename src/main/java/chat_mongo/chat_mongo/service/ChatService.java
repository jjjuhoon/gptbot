package chat_mongo.chat_mongo.service;

import chat_mongo.chat_mongo.entity.Chat;
import chat_mongo.chat_mongo.repository.ChatRepository;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {
    private final OpenAiService openAiService;
    private final ChatRepository chatRepository;

    public ChatService(ChatRepository chatRepository, @Value("${openai.api.key}") String apiKey) {
        this.openAiService = new OpenAiService(apiKey);
        this.chatRepository = chatRepository;
    }

    public Flux<Object> openAIChatStream(String content) {
        String prompt = content + " 이거 번역해줘";
        ChatMessage userMessage = new ChatMessage("user", prompt);
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(userMessage))
                .stream(true)
                .build();

        return Flux.create(emitter -> {
            try {
                openAiService.streamChatCompletion(request)
                        .doOnError(emitter::error)
                        .subscribe(emitter::next, emitter::error, emitter::complete);
            } catch (Exception e) {
                emitter.error(e);
            }
        }).onErrorResume(e -> {
            return Flux.error(new RuntimeException("OpenAI API 요청 중 오류 발생: " + e.getMessage()));
        });
    }

    public Mono<Chat> saveChat(Chat chat) {
        chat.setCreatedAt(LocalDateTime.now());
        return chatRepository.save(chat);
    }
}
