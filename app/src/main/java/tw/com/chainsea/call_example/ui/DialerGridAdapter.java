package tw.com.chainsea.call_example.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import tw.com.chainsea.call_example.R;
import tw.com.chainsea.call_example.entity.DigitModel;

/**
 * DialerGridAdapter
 * Created by 90Chris on 2015/3/6.
 */
public class DialerGridAdapter extends BaseAdapter {
    Activity mActivity = null;
    List<DigitModel> digitList = null;

    public DialerGridAdapter(Activity mActivity, List<DigitModel> digitList) {
        this.mActivity = mActivity;
        this.digitList = digitList;
    }

    @Override
    public int getCount() {
        return digitList.size();
    }

    @Override
    public DigitModel getItem(int i) {
        return digitList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = mActivity.getLayoutInflater().inflate(R.layout.item_digit, viewGroup, false);
        TextView numText = (TextView)view.findViewById(R.id.digit_num);
        numText.setText(getItem(i).getNum());
        TextView subText = (TextView)view.findViewById(R.id.digit_sub);
        subText.setText(getItem(i).getSub());
        return view;
    }
}
