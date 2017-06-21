package pl.nwg.dev.rambler.gpx;

/**
 * Created by piotrm on 22.05.17.
 */

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CustomListNoIcons extends ArrayAdapter<String>{

    private final Activity context;
    private final String[] web;

    public CustomListNoIcons(Activity context,
                             String[] web) {
        super(context, R.layout.list_single_no_icons, web);
        this.context = context;
        this.web = web;

    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView= inflater.inflate(R.layout.list_single_no_icons, null, true);
        TextView txtTitle = (TextView) rowView.findViewById(R.id.txt);

        ImageView imageView = (ImageView) rowView.findViewById(R.id.img);
        txtTitle.setText(web[position]);

        return rowView;
    }
}
