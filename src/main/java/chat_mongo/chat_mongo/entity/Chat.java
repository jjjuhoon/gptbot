package chat_mongo.chat_mongo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection="chat")
public class Chat {
    @Id
    private String id;
    private String msg;
    private String sender;
    private String receiver;
    private LocalDateTime createdAt;
}
