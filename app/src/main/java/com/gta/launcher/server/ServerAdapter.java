package com.gta.launcher.server;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gta.game.R;

import java.util.ArrayList;
import java.util.List;

public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ServerViewHolder> {

    public interface OnServerClickListener {
        void onServerClick(ServerInfo server);
        void onConnectClick(ServerInfo server);
        void onFavoriteToggle(ServerInfo server, boolean isFavorite);
    }

    private final Context context;
    private final List<ServerInfo> allServers = new ArrayList<>();
    private final List<ServerInfo> displayedServers = new ArrayList<>();
    private final OnServerClickListener listener;

    private String currentSearch = "";
    private boolean filterHideEmpty = false;
    private boolean filterNoPassword = false;

    public ServerAdapter(Context context, OnServerClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setServers(List<ServerInfo> servers) {
        allServers.clear();
        if (servers != null) {
            allServers.addAll(servers);
        }
        applyFilters();
    }

    public void setSearchQuery(String query) {
        this.currentSearch = query != null ? query.trim().toLowerCase() : "";
        applyFilters();
    }

    public void setHideEmpty(boolean hideEmpty) {
        this.filterHideEmpty = hideEmpty;
        applyFilters();
    }

    public void setNoPassword(boolean noPassword) {
        this.filterNoPassword = noPassword;
        applyFilters();
    }

    private void applyFilters() {
        displayedServers.clear();
        for (ServerInfo s : allServers) {
            if (filterHideEmpty && s.getPlayers() <= 0) {
                continue;
            }
            if (filterNoPassword && s.isHasPassword()) {
                continue;
            }
            if (!currentSearch.isEmpty()) {
                boolean matchName = s.getCleanHostname().toLowerCase().contains(currentSearch);
                boolean matchIp = s.getAddress().toLowerCase().contains(currentSearch);
                boolean matchGm = s.getGamemode().toLowerCase().contains(currentSearch);
                boolean matchLa = s.getLanguage().toLowerCase().contains(currentSearch);
                if (!matchName && !matchIp && !matchGm && !matchLa) {
                    continue;
                }
            }
            displayedServers.add(s);
        }
        notifyDataSetChanged();
    }

    public int getDisplayedCount() {
        return displayedServers.size();
    }

    public List<ServerInfo> getDisplayedServers() {
        return displayedServers;
    }

    @NonNull
    @Override
    public ServerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_server_card, parent, false);
        return new ServerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServerViewHolder holder, int position) {
        ServerInfo server = displayedServers.get(position);
        holder.bind(server);
    }

    @Override
    public int getItemCount() {
        return displayedServers.size();
    }

    class ServerViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvHostname;
        private final TextView tvAddress;
        private final TextView tvGamemode;
        private final TextView tvLanguage;
        private final TextView tvPlayers;
        private final TextView tvPing;
        private final ImageView ivPassword;
        private final ImageButton btnFavorite;
        private final Button btnConnect;

        public ServerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHostname = itemView.findViewById(R.id.tvHostname);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvGamemode = itemView.findViewById(R.id.tvGamemode);
            tvLanguage = itemView.findViewById(R.id.tvLanguage);
            tvPlayers = itemView.findViewById(R.id.tvPlayers);
            tvPing = itemView.findViewById(R.id.tvPing);
            ivPassword = itemView.findViewById(R.id.ivPassword);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            btnConnect = itemView.findViewById(R.id.btnConnect);
        }

        public void bind(final ServerInfo server) {
            tvHostname.setText(server.getCleanHostname());
            tvAddress.setText(server.getAddress());
            tvGamemode.setText(server.getGamemode());
            tvLanguage.setText(server.getLanguage());

            tvPlayers.setText(server.getPlayers() + " / " + server.getMaxPlayers());
            if (server.getMaxPlayers() > 0 && server.getPlayers() >= server.getMaxPlayers()) {
                tvPlayers.setTextColor(Color.parseColor("#FF5252"));
            } else if (server.getPlayers() > 0) {
                tvPlayers.setTextColor(Color.parseColor("#00E676"));
            } else {
                tvPlayers.setTextColor(Color.parseColor("#888888"));
            }

            if (server.getPing() >= 0) {
                tvPing.setVisibility(View.VISIBLE);
                tvPing.setText(server.getPing() + "ms");
                if (server.getPing() < 100) {
                    tvPing.setTextColor(Color.parseColor("#00E676"));
                } else if (server.getPing() < 200) {
                    tvPing.setTextColor(Color.parseColor("#FFD600"));
                } else {
                    tvPing.setTextColor(Color.parseColor("#FF5252"));
                }
            } else {
                tvPing.setVisibility(View.GONE);
            }

            ivPassword.setVisibility(server.isHasPassword() ? View.VISIBLE : View.GONE);

            btnFavorite.setImageResource(server.isFavorite() ? R.drawable.ic_star_filled : R.drawable.ic_star_border);

            btnFavorite.setOnClickListener(v -> {
                boolean newFav = !server.isFavorite();
                server.setFavorite(newFav);
                btnFavorite.setImageResource(newFav ? R.drawable.ic_star_filled : R.drawable.ic_star_border);
                if (listener != null) {
                    listener.onFavoriteToggle(server, newFav);
                }
            });

            btnConnect.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConnectClick(server);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onServerClick(server);
                }
            });
        }
    }
}
