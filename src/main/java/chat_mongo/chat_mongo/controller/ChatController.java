package chat_mongo.chat_mongo.controller;

import chat_mongo.chat_mongo.entity.Chat;
import chat_mongo.chat_mongo.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.awt.*;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@RestController
public class ChatController {

    private final ChatRepository chatRepository;

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
}
