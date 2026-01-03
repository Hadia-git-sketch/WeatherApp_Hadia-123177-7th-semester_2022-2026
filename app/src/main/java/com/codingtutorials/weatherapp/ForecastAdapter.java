package com.codingtutorials.weatherapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder> {

    private final Context context;
    private final List<ForecastItem> forecastList;
    private final boolean isDaily;
    private final String cityName; // Added to handle specific Google searches

    public ForecastAdapter(Context context, List<ForecastItem> forecastList, boolean isDaily, String cityName) {
        this.context = context;
        this.forecastList = forecastList;
        this.isDaily = isDaily;
        this.cityName = cityName;
    }

    @NonNull
    @Override
    public ForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isDaily ? R.layout.item_daily_forecast : R.layout.item_hourly_forecast;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ForecastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastViewHolder holder, int position) {
        ForecastItem item = forecastList.get(position);

        holder.timeOrDayText.setText(item.getTimeOrDay());

        // Set the appropriate layout data (Temp vs Min/Max)
        if (isDaily) {
            holder.dailyTempRange.setText(String.format(Locale.getDefault(), "%.0f°/%.0f°", item.getMaxTemp(), item.getMinTemp()));
            holder.hourlyTemp.setVisibility(View.GONE);
            holder.dailyTempRange.setVisibility(View.VISIBLE);
        } else {
            holder.hourlyTemp.setText(String.format(Locale.getDefault(), "%.0f°", item.getTemp()));
            holder.hourlyTemp.setVisibility(View.VISIBLE);
            holder.dailyTempRange.setVisibility(View.GONE);
        }

        // Set weather icon
        String resourceName = "ic_" + item.getIconCode();
        int resId = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
        if (resId != 0) {
            holder.icon.setImageResource(resId);
        } else {
            holder.icon.setImageResource(R.drawable.ic_01d);
        }

        // REDIRECT TO GOOGLE ON CLICK
        holder.itemView.setOnClickListener(v -> {
            String forecastType = isDaily ? "7+day+forecast" : "24+hour+forecast";
            String query = "weather+in+" + cityName + "+" + forecastType;
            String url = "https://www.google.com/search?q=" + query;

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return forecastList.size();
    }

    public static class ForecastViewHolder extends RecyclerView.ViewHolder {
        TextView timeOrDayText;
        ImageView icon;
        TextView hourlyTemp;
        TextView dailyTempRange;

        public ForecastViewHolder(@NonNull View itemView) {
            super(itemView);
            timeOrDayText = itemView.findViewById(R.id.timeOrDayText);
            icon = itemView.findViewById(R.id.forecastIcon);
            hourlyTemp = itemView.findViewById(R.id.hourlyTempText);
            dailyTempRange = itemView.findViewById(R.id.dailyTempRangeText);
        }
    }
}