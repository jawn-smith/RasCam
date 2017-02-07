package rpi.rpicam;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import java.util.ArrayList;

public class CustomAdapter extends ArrayAdapter<Model>{

    ArrayList<Model> modelItems = null;
    //Model[] modelItems = null;
    Context context;

    public CustomAdapter(Context context,int layoutResourceId, ArrayList<Model> resource) {
        super(context,R.layout.row,resource);
        // TODO Auto-generated constructor stub
        this.context = context;
        this.modelItems = resource;
        //this.modelItems = modelItems;
    }
    /*@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        convertView = inflater.inflate(R.layout.row, parent, false);
        TextView name = (TextView) convertView.findViewById(R.id.textView1);
        CheckBox cb = (CheckBox) convertView.findViewById(R.id.checkBox1);
        name.setText(modelItems.get(position));
        cb.setChecked(false);
        return convertView;
    }*/
    static class ViewHolderItem {
        TextView textViewItem;
        CheckBox checkBox;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolderItem viewHolder;

        if(convertView==null){

            // inflate the layout
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            convertView = inflater.inflate(R.layout.row, parent, false);

            // well set up the ViewHolder
            viewHolder = new ViewHolderItem();
            viewHolder.textViewItem = (TextView) convertView.findViewById(R.id.textView1);
            viewHolder.checkBox = (CheckBox) convertView.findViewById(R.id.checkBox1);
            // store the holder with the view.
            convertView.setTag(viewHolder);

        }else{
            // we've just avoided calling findViewById() on resource everytime
            // just use the viewHolder
            viewHolder = (ViewHolderItem) convertView.getTag();
        }

        // object item based on the position
        Model model = modelItems.get(position);

        // assign values if the object is not null
        if(model != null) {
            // get the TextView from the ViewHolder and then set the text (item name) and tag (item ID) values
            viewHolder.textViewItem.setText(model.filename);

            if (model.cbShowing){
                viewHolder.checkBox.setVisibility(View.VISIBLE);
            }
            else{
                viewHolder.checkBox.setVisibility(View.GONE);
            }
            if (model.cbChecked){
                viewHolder.checkBox.setChecked(true);
            }
            else{
                viewHolder.checkBox.setChecked(false);
            }
        }

        return convertView;

    }
}
