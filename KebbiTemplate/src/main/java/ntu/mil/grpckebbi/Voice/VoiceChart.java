/*This file is just for Demo: VoiceRecorder+GoogleS2T. */

package ntu.mil.grpckebbi.Voice;

import android.graphics.Color;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class VoiceChart {
    private LineDataSet createSet(String SetName) {
        LineDataSet set = new LineDataSet(null, SetName);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.GRAY);
        set.setLineWidth(2);
        set.setDrawCircles(false);
        set.setFillColor(Color.RED);
        set.setFillAlpha(50);
        set.setDrawFilled(true);
        set.setValueTextColor(Color.BLACK);
        set.setDrawValues(false);
        return set;
    }

    private void initChart(LineChart mLineChart){
        mLineChart.getDescription().setEnabled(false);// Tag
        mLineChart.setTouchEnabled(true);// Touchable
        mLineChart.setDragEnabled(true);// Interactive

        // Set a basic line
        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);
        mLineChart.setData(data);

        // Bottom left tags
        Legend l =  mLineChart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.BLACK);

        // X Axis
        XAxis x =  mLineChart.getXAxis();
        x.setTextColor(Color.BLACK);
        x.setDrawGridLines(true);//畫X軸線
        x.setPosition(XAxis.XAxisPosition.BOTTOM);//把標籤放底部
        x.setLabelCount(5,true);//設置顯示5個標籤

        // Content of X axis
        x.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "No. "+Math.round(value);
            }
        });

        YAxis y = mLineChart.getAxisLeft();
        y.setTextColor(Color.BLACK);
        y.setDrawGridLines(true);
        y.setAxisMaximum(16000);
        y.setAxisMinimum(0);
        mLineChart.getAxisRight().setEnabled(false);// Right YAxis invisible
        mLineChart.setVisibleXRange(0,100);// Set visible range
    }

    private void addData(int inputData, LineChart mLineChart){
        LineData data =  mLineChart.getData();

        // DB data only, set to 0. If other type of data be added, set to other int.
        ILineDataSet set = data.getDataSetByIndex(0);
        if (set == null){
            set = createSet("DB_DATA");
            data.addDataSet(set);
        }
        data.addEntry(new Entry(set.getEntryCount(),inputData),0);

        // Renew plot
        data.notifyDataChanged();
        mLineChart.notifyDataSetChanged();
        mLineChart.setVisibleXRange(0,100); // Visible range
        mLineChart.moveViewToX(data.getEntryCount());// Whether track on the newest data point
    }
}
