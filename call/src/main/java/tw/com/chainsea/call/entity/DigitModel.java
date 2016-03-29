package tw.com.chainsea.call.entity;

/**
 * DigitModel
 * Created by 90Chris on 2015/3/9.
 */
public class DigitModel {
    String num;  //数字
    String sub;  //底部字母
    public DigitModel(String num, String sub) {
        this.num = num;
        this.sub = sub;
    }
    public String getNum() {
        return num;
    }
    public String getSub() {
        return sub;
    }
}