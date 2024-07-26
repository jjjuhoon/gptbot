package chat_mongo.chat_mongo.controller;

import chat_mongo.chat_mongo.DTO.MessageDTO;
import chat_mongo.chat_mongo.entity.Chat;
import chat_mongo.chat_mongo.repository.ChatRepository;
import chat_mongo.chat_mongo.service.ChatService;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.LocalDateTime;


@RequiredArgsConstructor
@RestController
public class ChatController {

    private final ChatRepository chatRepository;
    private final ChatService chatService;

    @GetMapping(value="/sender/{sender}/receiver/{receiver}",produces= MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Chat> getMsg(@PathVariable String sender, @PathVariable String receiver){
        return chatRepository.mFindBySender(sender,receiver)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/chat")
    public Mono<Chat> setMsg(@RequestBody Chat chat){
        chat.setCreatedAt((LocalDateTime.now()));
        return chatRepository.save(chat);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody MessageDTO messageDto) {
        // 사용자 메시지 저장
        Chat chat = new Chat();
        chat.setMsg(messageDto.getMsg());
        chat.setCreatedAt(LocalDateTime.now());
        chat.setSender("user");
        chat.setReceiver("gpt");
        chatService.saveChat(chat).subscribe();

        // GPT 응답 스트리밍 및 저장
        return chatService.openAIChatStream(messageDto.getMsg())
                .flatMap(chunk -> {
                    if (chunk instanceof ChatCompletionChunk) {
                        ChatCompletionChunk completionChunk = (ChatCompletionChunk) chunk;
                        if (!completionChunk.getChoices().isEmpty()) {
                            String content = completionChunk.getChoices().get(0).getMessage().getContent();
                            Chat newChat = Chat.builder()
                                    .msg(content)
                                    .sender("gpt")
                                    .receiver("user")
                                    .createdAt(LocalDateTime.now())
                                    .build();
                            // GPT 응답 저장
                            return chatService.saveChat(newChat)
                                    .thenMany(Flux.fromArray(content.split("")));
                        }
                    }
                    return Flux.empty();
                })
                .map(charSeq -> "data: " + charSeq + "\n\n");
    }
}