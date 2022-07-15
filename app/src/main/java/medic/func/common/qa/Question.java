package medic.func.common.qa;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static medic.core.Utils.getRandomInt;

/**
 * @author MengLeiFudge
 */
@Data
public class Question implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String question;
    private final List<Answer> answers = new ArrayList<>();

    Question(String question, String answer, long qq) {
        this.question = question;
        answers.add(new Answer(answer, qq));
    }

    Answer getAnswer(String a) {
        for (Answer answer : answers) {
            if (answer.getAnswer().equals(a)) {
                return answer;
            }
        }
        return null;
    }

    void add(String a, long qq) {
        Answer answer = getAnswer(a);
        if (answer == null) {
            answers.add(new Answer(a, qq));
            return;
        }
        answer.add(qq);
    }

    String getRandomAnswer() {
        return (String) answers.toArray()[getRandomInt(0, answers.size() - 1)];
    }
}
