package pl.lodz.p.it.wks.wksrecruiter.collections;

public class AttemptAnswer {
    private int questionNumber;
    private String answer;

    public AttemptAnswer(){}
    public AttemptAnswer(int questionNumber, String answer) {
        this.questionNumber = questionNumber;
        this.answer = answer;
    }
    public int getQuestionNumber() { return questionNumber; }
    public void setQuestionNumber(int questionNumber) { this.questionNumber = questionNumber; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
}