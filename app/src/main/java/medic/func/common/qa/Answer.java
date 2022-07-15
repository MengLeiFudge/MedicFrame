package medic.func.common.qa;

import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author MengLeiFudge
 */
@Data
public class Answer implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String answer;
    private final Set<Long> qqs = new HashSet<>();

    Answer(String answer, long qq) {
        this.answer = answer;
        qqs.add(qq);
    }

    void add(long qq) {
        qqs.add(qq);
    }
}
