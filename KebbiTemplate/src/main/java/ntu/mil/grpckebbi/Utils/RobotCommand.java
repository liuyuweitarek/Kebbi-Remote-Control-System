package ntu.mil.grpckebbi.Utils;

public class RobotCommand {
    private String intent;
    private String expression;
    private String value;

    public RobotCommand(String intent, String value){
        this.intent = intent;
        this.value = value;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "RobotCommand{" +
                "intent='" + intent + '\'' +
                ", expression='" + expression + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}

