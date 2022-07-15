package medic.func.common.qa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author MengLeiFudge
 */
public class QaList implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Question> questions = new ArrayList<>();

    QaList() {
    }

    Question getQuestion(String q) {
        for (Question question : questions) {
            if (question.getQuestion().equals(q)) {
                return question;
            }
        }
        return null;
    }

/*
    void add(String q, String a, long qq) {
        Question question = getQuestion(q);
        if (question == null) {
            questions.add(new Question(q, a, qq));
            return;
        }
        question.add(a, qq);
    }

    /**
     * 移除指定位置的问答.
     * @param qIndex 从0开始的问题位置
     * @param aIndex 从0开始的回答位置
     * @param qq 进行移除操作的qq
     * @return 移除结果
     */
    /*
    public boolean remove(int qIndex, int aIndex, long qq) {
        if (qIndex >= questions.size()) {
            return false;
        }

        if (this.qqList.get(index) != qq) {
            return false;
        }
        questions.remove(index);
        if (questions.isEmpty()) {
            deleteIfExists(getQaFile() + question);
        }
        return true;
    }

    public String getRandomAnswer(String q) {
        Question question = getQuestion(q);
        if (question == null) {
            return null;
        }
        return question.getRandomAnswer();
    }


    public void sortList() {
        list.sort(Comparator.comparingInt(o -> o.getQuestion().charAt(0)));
    }

    public List<Question> getSortedList() {
        Arrays.sort();
        return list;
    }

    public int getSize() {
        return list.size();
    }

    public void add(String question, String answer, long addQQ) {
        for (int i = 0; i < list.size(); i++) {
            Process base = list.get(i);
            if (base.getQuestion().equals(question)) {
                base.add(answer, addQQ);
                list.set(i, base);
                return;
            }
        }
        Process base = new Process(question);
        base.add(answer, addQQ);
        list.add(base);
    }

    public void set(int index, Process base) {
        list.set(index, base);
    }

    public void remove(int index) {
        sortList();
        list.remove(index);
    }*/
}
