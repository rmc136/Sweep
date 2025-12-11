package com.sweepgame.server.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class MoveDTO {
    
    private String sessionId;
    private String username;
    private int handCardIndex;
    private List<Integer> tableCardIndices;
    
    public MoveDTO() {
    }
    
    public MoveDTO(String sessionId, String username, int handCardIndex, List<Integer> tableCardIndices) {
        this.sessionId = sessionId;
        this.username = username;
        this.handCardIndex = handCardIndex;
        this.tableCardIndices = tableCardIndices;
    }
}
