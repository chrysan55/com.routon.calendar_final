package com.routon.calendar;

import java.util.List;

import com.routon.calendar.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

class DaysAdapter extends BaseAdapter{ 
	
	private class GridHolder{
		ImageView spot;
		TextView GC;
		TextView LC;
		TextView MD;
	}
    private LayoutInflater inflater; 
    private List<GridInfo> gridinfo; 
    private Context context;
 
    public DaysAdapter(Context context) 
    { 
        super(); 
        this.context = context;
    } 
    
    public void setList(List<GridInfo> list){
    	this.gridinfo = list;
    	inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
 
    public int getCount() 
    { 
        if (null != gridinfo) 
        { 
            return gridinfo.size(); 
        } else
        { 
            return 0; 
        } 
    } 
 
    public Object getItem(int index) 
    { 
        return gridinfo.get(index); 
    } 
 
    public long getItemId(int index) 
    { 
        return index; 
    } 
 
    public View getView(int position, View convertView, ViewGroup parent) 
    { 
        GridHolder GridHolder; 
        if (convertView == null) 
        { 
            convertView = inflater.inflate(R.layout.grid_item, null); 
            GridHolder = new GridHolder(); 
            GridHolder.spot = (ImageView) convertView.findViewById(R.id.spot); 
            GridHolder.GC = (TextView) convertView.findViewById(R.id.GC);
            GridHolder.LC = (TextView) convertView.findViewById(R.id.LC); 
            GridHolder.MD = (TextView) convertView.findViewById(R.id.MD); 
            convertView.setTag(GridHolder); 
        } else
        { 
            GridHolder = (GridHolder) convertView.getTag(); 
        } 
        GridHolder.GC.setText(gridinfo.get(position).getGC());
        GridHolder.LC.setText(gridinfo.get(position).getLC());
        GridHolder.MD.setText(gridinfo.get(position).getMD());
//        GridHolder.spot.setImageResource(R.drawable.spot);
        int color = gridinfo.get(position).getColor();
        GridHolder.GC.setTextColor(color);
        GridHolder.LC.setTextColor(color);
        GridHolder.MD.setTextColor(color);
        return convertView; 
    } 
 
}
