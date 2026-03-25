package object;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Objects;

public class Door_Object extends SuperObject {

    public Door_Object() {
        name = "Door";
        try {
            image = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/objects/door.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        collision = true;
    }

}
