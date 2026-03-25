package object;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Objects;

public class Key_Object extends SuperObject {

    public Key_Object() {
        name = "Key";
        try {
            image = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/objects/key.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
