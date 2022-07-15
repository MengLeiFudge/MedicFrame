package medic.core;

import java.util.ArrayList;
import java.util.List;

import static medic.core.Api.textMsg;

/**
 * @author MengLeiFudge
 */
public abstract class ICommand {
    protected List<String> alias = new ArrayList<>();

    boolean matches() {
        for (String s : alias) {
            if (textMsg.matches(s)) {
                return true;
            }
        }
        return false;
    }

    void before() {
    }

    protected abstract void run();

    void after() {
    }

}
