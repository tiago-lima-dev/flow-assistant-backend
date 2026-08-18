package br.com.flow_assistant.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentBlock {

    private String type;
    private String text;
    private String id;
    private String name;
    private JsonNode input;
    private String toolUseId;
    private String content;
    private Boolean isError;

    public ContentBlock() {
    }

    private ContentBlock(String type) {
        this.type = type;
    }

    public static ContentBlock text(String text) {
        ContentBlock block = new ContentBlock("text");
        block.text = text;
        return block;
    }

    public static ContentBlock toolResult(String toolUseId, String content) {
        ContentBlock block = new ContentBlock("tool_result");
        block.toolUseId = toolUseId;
        block.content = content;
        return block;
    }

    @JsonIgnore
    public boolean isToolUseBlock() {
        return "tool_use".equals(type);
    }

    @JsonIgnore
    public boolean isTextBlock() {
        return "text".equals(type);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JsonNode getInput() {
        return input;
    }

    public void setInput(JsonNode input) {
        this.input = input;
    }

    public String getToolUseId() {
        return toolUseId;
    }

    public void setToolUseId(String toolUseId) {
        this.toolUseId = toolUseId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getIsError() {
        return isError;
    }

    public void setIsError(Boolean isError) {
        this.isError = isError;
    }
}
