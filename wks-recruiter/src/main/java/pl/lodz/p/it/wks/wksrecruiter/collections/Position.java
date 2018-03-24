package pl.lodz.p.it.wks.wksrecruiter.collections;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "positions")
public class Position {
    @Id
    private String id;
    private String name;
    private boolean active;

    public Position(){}
    public Position(String name, boolean active) {
        this.name = name;
        this.active = active;
    }
    public Position(String id, String name, boolean active) {
        this.id = id;
        this.name = name;
        this.active = active;
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
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
}
