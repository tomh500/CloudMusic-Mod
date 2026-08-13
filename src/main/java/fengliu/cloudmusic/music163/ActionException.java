package fengliu.cloudmusic.music163;

import net.minecraft.network.chat.Component;

public class ActionException extends RuntimeException {

    public ActionException(String msg){
        super(msg);
    }

    public ActionException(Component text){
        super(text.getString());
    }
}
