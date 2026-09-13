package com.gta.launcher.activity;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gta.game.R;
import com.gta.game.SAMP;
import com.gta.launcher.server.SampQueryClient;
import com.gta.launcher.server.ServerAdapter;
import com.gta.launcher.server.ServerInfo;
import com.gta.launcher.server.ServerRepository;
import com.gta.launcher.server.SettingsIniManager;

import java.util.List;

public class ServerBrowserActivity extends AppCompatActivity implements ServerAdapter.OnServerClickListener {

    private static final String TAG = "ServerBrowserActivity";

    private enum TabType {
        INTERNET, FAVORITES, RECENTS, CURATED
    }

    private TabType currentTab = TabType.INTERNET;

    private ServerRepository repository;
    private ServerAdapter adapter;

    private TextView tvServerCount;
    private TextView tvPlayerName;
    private EditText etSearch;
    private Button tabInternet, tabFavorites, tabRecents, tabCurated;
    private CheckBox cbHideEmpty, cbNoPassword;
    private RecyclerView rvServers;
    private LinearLayout layoutLoading;
    private TextView tvLoadingMessage;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyTitle, tvEmptyDescription;
    private TextView tvStatusServer;
    private Button btnQuickConnect;

    private ServerInfo selectedServer;
    private boolean storagePermissionGranted = false;
    private ServerInfo pendingConnectServer = null;
    private String pendingConnectPassword = null;

    private final ActivityResultLauncher<String[]> requestStoragePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = true;
                for (Boolean granted : permissions.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted) {
                    storagePermissionGranted = true;
                    if (pendingConnectServer != null) {
                        performConnect(pendingConnectServer, pendingConnectPassword);
                    }
                } else {
                    storagePermissionGranted = false;
                    Toast.makeText(this, "Storage permission is required for SA-MP to load settings", Toast.LENGTH_LONG).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        requestManageStoragePermission();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> manageStorageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        storagePermissionGranted = true;
                        if (pendingConnectServer != null) {
                            performConnect(pendingConnectServer, pendingConnectPassword);
                        }
                    } else {
                        storagePermissionGranted = false;
                        Toast.makeText(this, "Storage access is required to play SA-MP", Toast.LENGTH_LONG).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreenMode();
        setContentView(R.layout.activity_server_browser);

        repository = new ServerRepository(this);
        checkStoragePermissionInitial();

        initViews();
        setupListeners();

        updateNicknameDisplay();
        switchTab(TabType.INTERNET);
    }

    private void setFullScreenMode() {
        try {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            );
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                WindowManager.LayoutParams params = getWindow().getAttributes();
                params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                getWindow().setAttributes(params);
            }
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting fullscreen: " + e.getMessage());
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setFullScreenMode();
        }
    }

    private void checkStoragePermissionInitial() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            storagePermissionGranted = Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            storagePermissionGranted = ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            storagePermissionGranted = true;
        }
    }

    private void requestManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                manageStorageLauncher.launch(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                manageStorageLauncher.launch(intent);
            }
        }
    }

    private void ensureStoragePermissionThen(ServerInfo server, String password) {
        pendingConnectServer = server;
        pendingConnectPassword = password;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                storagePermissionGranted = true;
                performConnect(server, password);
            } else {
                requestManageStoragePermission();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions = new String[]{
                        android.Manifest.permission.READ_MEDIA_IMAGES,
                        android.Manifest.permission.READ_MEDIA_VIDEO,
                        android.Manifest.permission.READ_MEDIA_AUDIO
                };
            } else {
                permissions = new String[]{
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                };
            }

            boolean allGranted = true;
            for (String perm : permissions) {
                if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                storagePermissionGranted = true;
                performConnect(server, password);
            } else {
                requestStoragePermissionLauncher.launch(permissions);
            }
        } else {
            storagePermissionGranted = true;
            performConnect(server, password);
        }
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvServerCount = findViewById(R.id.tvServerCount);
        tvPlayerName = findViewById(R.id.tvPlayerName);
        etSearch = findViewById(R.id.etSearch);

        tabInternet = findViewById(R.id.tabInternet);
        tabFavorites = findViewById(R.id.tabFavorites);
        tabRecents = findViewById(R.id.tabRecents);
        tabCurated = findViewById(R.id.tabCurated);

        cbHideEmpty = findViewById(R.id.cbHideEmpty);
        cbNoPassword = findViewById(R.id.cbNoPassword);

        rvServers = findViewById(R.id.rvServers);
        rvServers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ServerAdapter(this, this);
        rvServers.setAdapter(adapter);

        layoutLoading = findViewById(R.id.layoutLoading);
        tvLoadingMessage = findViewById(R.id.tvLoadingMessage);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptyDescription = findViewById(R.id.tvEmptyDescription);

        tvStatusServer = findViewById(R.id.tvStatusServer);
        btnQuickConnect = findViewById(R.id.btnQuickConnect);

        // Preload configured server if any
        ServerInfo lastServer = SettingsIniManager.getCurrentConfiguredServer(this);
        if (lastServer != null) {
            selectServer(lastServer);
        }
    }

    private void setupListeners() {
        // Nickname click
        findViewById(R.id.btnNickname).setOnClickListener(v -> showEditNicknameDialog());

        // Add server button
        findViewById(R.id.btnAddServer).setOnClickListener(v -> showAddServerDialog());

        // Refresh button
        findViewById(R.id.btnRefresh).setOnClickListener(v -> refreshCurrentTab(true));

        // Tab switches
        tabInternet.setOnClickListener(v -> switchTab(TabType.INTERNET));
        tabFavorites.setOnClickListener(v -> switchTab(TabType.FAVORITES));
        tabRecents.setOnClickListener(v -> switchTab(TabType.RECENTS));
        tabCurated.setOnClickListener(v -> switchTab(TabType.CURATED));

        // Search text watcher
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.setSearchQuery(s.toString());
                updateCountAndEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Filter toggles
        cbHideEmpty.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adapter.setHideEmpty(isChecked);
            updateCountAndEmptyState();
        });

        cbNoPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adapter.setNoPassword(isChecked);
            updateCountAndEmptyState();
        });

        // Quick Connect
        btnQuickConnect.setOnClickListener(v -> {
            if (selectedServer != null) {
                connectToServer(selectedServer, null);
            }
        });
    }

    private void updateNicknameDisplay() {
        String nick = SettingsIniManager.getPlayerNickname(this);
        tvPlayerName.setText(nick);
    }

    private void switchTab(TabType tab) {
        this.currentTab = tab;

        // Reset tab styles
        tabInternet.setBackgroundResource(tab == TabType.INTERNET ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
        tabInternet.setTextColor(tab == TabType.INTERNET ? Color.WHITE : Color.parseColor("#CCCCCC"));

        tabFavorites.setBackgroundResource(tab == TabType.FAVORITES ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
        tabFavorites.setTextColor(tab == TabType.FAVORITES ? Color.WHITE : Color.parseColor("#CCCCCC"));

        tabRecents.setBackgroundResource(tab == TabType.RECENTS ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
        tabRecents.setTextColor(tab == TabType.RECENTS ? Color.WHITE : Color.parseColor("#CCCCCC"));

        tabCurated.setBackgroundResource(tab == TabType.CURATED ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
        tabCurated.setTextColor(tab == TabType.CURATED ? Color.WHITE : Color.parseColor("#CCCCCC"));

        refreshCurrentTab(false);
    }

    private void refreshCurrentTab(boolean forceNetworkRefresh) {
        layoutEmpty.setVisibility(View.GONE);

        switch (currentTab) {
            case INTERNET:
                loadInternetServers(forceNetworkRefresh);
                break;
            case FAVORITES:
                loadFavorites();
                break;
            case RECENTS:
                loadRecents();
                break;
            case CURATED:
                loadCurated();
                break;
        }
    }

    private void loadInternetServers(boolean forceRefresh) {
        layoutLoading.setVisibility(View.VISIBLE);
        tvLoadingMessage.setText("Fetching SA-MP servers...");
        rvServers.setVisibility(View.INVISIBLE);

        repository.fetchInternetServers(forceRefresh, new ServerRepository.ServerListCallback() {
            @Override
            public void onLoaded(List<ServerInfo> servers) {
                layoutLoading.setVisibility(View.GONE);
                rvServers.setVisibility(View.VISIBLE);
                adapter.setServers(servers);
                updateCountAndEmptyState();
            }

            @Override
            public void onError(String message) {
                layoutLoading.setVisibility(View.GONE);
                rvServers.setVisibility(View.VISIBLE);
                updateCountAndEmptyState();
                Toast.makeText(ServerBrowserActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFavorites() {
        layoutLoading.setVisibility(View.GONE);
        rvServers.setVisibility(View.VISIBLE);
        List<ServerInfo> favs = repository.getFavorites();
        adapter.setServers(favs);
        updateCountAndEmptyState();
    }

    private void loadRecents() {
        layoutLoading.setVisibility(View.GONE);
        rvServers.setVisibility(View.VISIBLE);
        List<ServerInfo> recents = repository.getRecents();
        adapter.setServers(recents);
        updateCountAndEmptyState();
    }

    private void loadCurated() {
        layoutLoading.setVisibility(View.GONE);
        rvServers.setVisibility(View.VISIBLE);
        List<ServerInfo> curated = repository.loadLocalDefaultServers();
        adapter.setServers(curated);
        updateCountAndEmptyState();
    }

    private void updateCountAndEmptyState() {
        int count = adapter.getDisplayedCount();
        tvServerCount.setText("(" + count + ")");

        if (count == 0) {
            layoutEmpty.setVisibility(View.VISIBLE);
            if (currentTab == TabType.FAVORITES) {
                tvEmptyTitle.setText("No Favorite Servers");
                tvEmptyDescription.setText("Star any server from the list or tap 'Add IP' to save favorites here.");
            } else if (currentTab == TabType.RECENTS) {
                tvEmptyTitle.setText("No Recent Servers");
                tvEmptyDescription.setText("Servers you connect to will appear here for quick access.");
            } else {
                tvEmptyTitle.setText("No Servers Found");
                tvEmptyDescription.setText("Try changing your search terms or unchecking filter options.");
            }
        } else {
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void selectServer(ServerInfo server) {
        this.selectedServer = server;
        tvStatusServer.setText("Selected: " + server.getCleanHostname() + " (" + server.getAddress() + ")");
        btnQuickConnect.setEnabled(true);
    }

    @Override
    public void onServerClick(ServerInfo server) {
        selectServer(server);
        showServerDetailsDialog(server);
    }

    @Override
    public void onConnectClick(ServerInfo server) {
        selectServer(server);
        if (server.isHasPassword()) {
            showServerDetailsDialog(server);
        } else {
            connectToServer(server, null);
        }
    }

    @Override
    public void onFavoriteToggle(ServerInfo server, boolean isFavorite) {
        if (isFavorite) {
            repository.addFavorite(server);
            Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show();
        } else {
            repository.removeFavorite(server);
            Toast.makeText(this, "Removed from Favorites", Toast.LENGTH_SHORT).show();
            if (currentTab == TabType.FAVORITES) {
                loadFavorites();
            }
        }
    }

    private void connectToServer(ServerInfo server, String password) {
        ensureStoragePermissionThen(server, password);
    }

    private void performConnect(ServerInfo server, String password) {
        String nick = SettingsIniManager.getPlayerNickname(this);
        boolean written = SettingsIniManager.writeServerConnection(this, server, nick, password);

        repository.addRecent(server);

        Toast.makeText(this, "Connecting to " + server.getCleanHostname() + "...", Toast.LENGTH_SHORT).show();

        try {
            Intent gameIntent = new Intent(this, SAMP.class);
            startActivity(gameIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error launching SAMP: " + e.getMessage());
            Toast.makeText(this, "Failed to launch game: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showServerDetailsDialog(final ServerInfo server) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_server_details);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.75f),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }

        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);
        TextView dialogAddress = dialog.findViewById(R.id.dialogAddress);
        TextView dialogPing = dialog.findViewById(R.id.dialogPing);
        TextView dialogGamemode = dialog.findViewById(R.id.dialogGamemode);
        TextView dialogLanguage = dialog.findViewById(R.id.dialogLanguage);
        TextView dialogPlayers = dialog.findViewById(R.id.dialogPlayers);
        TextView dialogVersion = dialog.findViewById(R.id.dialogVersion);
        EditText dialogEtNickname = dialog.findViewById(R.id.dialogEtNickname);
        EditText dialogEtPassword = dialog.findViewById(R.id.dialogEtPassword);
        ImageButton dialogBtnFavorite = dialog.findViewById(R.id.dialogBtnFavorite);
        ImageButton dialogBtnCopy = dialog.findViewById(R.id.dialogBtnCopy);
        Button dialogBtnCancel = dialog.findViewById(R.id.dialogBtnCancel);
        Button dialogBtnConnect = dialog.findViewById(R.id.dialogBtnConnect);

        dialogTitle.setText(server.getCleanHostname());
        dialogAddress.setText(server.getAddress());
        dialogGamemode.setText(server.getGamemode());
        dialogLanguage.setText(server.getLanguage());
        dialogPlayers.setText(server.getPlayers() + " / " + server.getMaxPlayers());
        dialogVersion.setText(server.getVersion());

        String currentNick = SettingsIniManager.getPlayerNickname(this);
        dialogEtNickname.setText(currentNick);

        if (server.getPing() >= 0) {
            dialogPing.setText("Ping: " + server.getPing() + "ms");
        } else {
            dialogPing.setText("Ping: checking...");
            // Query server ping via SA-MP UDP protocol
            SampQueryClient.queryServer(server.getIp(), server.getPort(), new SampQueryClient.QueryCallback() {
                @Override
                public void onSuccess(ServerInfo updated) {
                    if (dialog.isShowing()) {
                        server.setPing(updated.getPing());
                        server.setPlayers(updated.getPlayers());
                        server.setMaxPlayers(updated.getMaxPlayers());
                        dialogPing.setText("Ping: " + updated.getPing() + "ms");
                        dialogPlayers.setText(updated.getPlayers() + " / " + updated.getMaxPlayers());
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (dialog.isShowing()) {
                        dialogPing.setText("Ping: --");
                    }
                }
            });
        }

        dialogBtnFavorite.setImageResource(server.isFavorite() ? R.drawable.ic_star_filled : R.drawable.ic_star_border);
        dialogBtnFavorite.setOnClickListener(v -> {
            boolean newFav = !server.isFavorite();
            onFavoriteToggle(server, newFav);
            dialogBtnFavorite.setImageResource(newFav ? R.drawable.ic_star_filled : R.drawable.ic_star_border);
            adapter.notifyDataSetChanged();
        });

        dialogBtnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Server IP", server.getAddress());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied IP: " + server.getAddress(), Toast.LENGTH_SHORT).show();
        });

        dialogBtnCancel.setOnClickListener(v -> dialog.dismiss());

        dialogBtnConnect.setOnClickListener(v -> {
            String newNick = dialogEtNickname.getText().toString().trim();
            if (!newNick.isEmpty()) {
                SettingsIniManager.setPlayerNickname(this, newNick);
                updateNicknameDisplay();
            }

            String password = dialogEtPassword.getText().toString().trim();
            dialog.dismiss();
            connectToServer(server, password);
        });

        dialog.show();
    }

    private void showAddServerDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_server);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.75f),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }

        EditText etAddHost = dialog.findViewById(R.id.etAddHost);
        EditText etAddPort = dialog.findViewById(R.id.etAddPort);
        EditText etAddName = dialog.findViewById(R.id.etAddName);
        Button btnAddTestPing = dialog.findViewById(R.id.btnAddTestPing);
        TextView tvAddPingResult = dialog.findViewById(R.id.tvAddPingResult);
        Button btnAddCancel = dialog.findViewById(R.id.btnAddCancel);
        Button btnAddSave = dialog.findViewById(R.id.btnAddSave);

        btnAddTestPing.setOnClickListener(v -> {
            String host = etAddHost.getText().toString().trim();
            String portStr = etAddPort.getText().toString().trim();
            if (host.isEmpty()) {
                Toast.makeText(this, "Please enter an IP or domain", Toast.LENGTH_SHORT).show();
                return;
            }
            int port = 7777;
            try {
                port = Integer.parseInt(portStr);
            } catch (Exception ignored) {}

            tvAddPingResult.setText("Querying server...");
            tvAddPingResult.setTextColor(Color.parseColor("#FFD600"));

            final int finalPort = port;
            SampQueryClient.queryServer(host, port, new SampQueryClient.QueryCallback() {
                @Override
                public void onSuccess(ServerInfo serverInfo) {
                    if (dialog.isShowing()) {
                        tvAddPingResult.setText("Online! " + serverInfo.getCleanHostname() + " (" + serverInfo.getPing() + "ms)");
                        tvAddPingResult.setTextColor(Color.parseColor("#00E676"));
                        if (etAddName.getText().toString().trim().isEmpty()) {
                            etAddName.setText(serverInfo.getCleanHostname());
                        }
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (dialog.isShowing()) {
                        tvAddPingResult.setText("Server did not respond (" + e.getMessage() + ")");
                        tvAddPingResult.setTextColor(Color.parseColor("#FF5252"));
                    }
                }
            });
        });

        btnAddCancel.setOnClickListener(v -> dialog.dismiss());

        btnAddSave.setOnClickListener(v -> {
            String host = etAddHost.getText().toString().trim();
            String portStr = etAddPort.getText().toString().trim();
            String name = etAddName.getText().toString().trim();

            if (host.isEmpty()) {
                Toast.makeText(this, "Please enter an IP or domain", Toast.LENGTH_SHORT).show();
                return;
            }

            int port = 7777;
            try {
                port = Integer.parseInt(portStr);
            } catch (Exception ignored) {}

            if (name.isEmpty()) {
                name = host + ":" + port;
            }

            ServerInfo newServer = new ServerInfo(host, port, name, 0, 100, "Custom", "EN", false, "0.3.7");
            repository.addFavorite(newServer);
            Toast.makeText(this, "Server saved to Favorites!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();

            switchTab(TabType.FAVORITES);
        });

        dialog.show();
    }

    private void showEditNicknameDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_edit_nickname);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.65f),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }

        EditText etNickname = dialog.findViewById(R.id.etNickname);
        etNickname.setText(SettingsIniManager.getPlayerNickname(this));
        etNickname.setSelection(etNickname.getText().length());

        dialog.findViewById(R.id.btnNickCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.findViewById(R.id.btnNickSave).setOnClickListener(v -> {
            String nick = etNickname.getText().toString().trim();
            if (nick.isEmpty()) {
                Toast.makeText(this, "Nickname cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            SettingsIniManager.setPlayerNickname(this, nick);
            updateNicknameDisplay();
            dialog.dismiss();
            Toast.makeText(this, "Nickname saved: " + nick, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
}
