
package com.novelarchive.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.novelarchive.app.database.UniverseRepository;
import com.novelarchive.app.database.UniverseRepository.Universe;
import com.novelarchive.app.database.UniverseRepository.Novel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.novelarchive.app.database.ArchiveSearchRepository;
import com.novelarchive.app.database.ArchiveSearchRepository.SearchResult;

import android.view.inputmethod.EditorInfo;

public class MainActivity extends Activity {

    private final int bg = Color.rgb(16, 19, 29);
    private final int card = Color.rgb(27, 33, 47);
    private final int accent = Color.rgb(177, 194, 255);
    private final int primary = Color.rgb(240, 242, 250);
    private final int secondary = Color.rgb(160, 169, 190);
    private final int danger = Color.rgb(255, 135, 135);
    
    private ArchiveSearchRepository searchRepository;
    private boolean showingSearchResults = false;
     
    private FrameLayout root;
    private LinearLayout drawer;
    private View scrim;
    private EditText searchInput;
    private LinearLayout content;

    private UniverseRepository repository;

    private boolean drawerOpen = false;
    private boolean searchOpen = false;

    private long currentUniverseId = -1;
    private String currentScreen = "HOME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);

        repository = new UniverseRepository(this);
        searchRepository = new ArchiveSearchRepository(this);

        buildHome();
    }

    // ==================================================
    // ROOT AND TOOLBAR
    // ==================================================

    private void buildBase(String title) {

        root = new FrameLayout(this);
        root.setBackgroundColor(bg);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);

        root.addView(main, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setPadding(dp(20), dp(12), dp(20), dp(12));

        main.addView(toolbar);

        FrameLayout menuButton = createIconButton(true);

        toolbar.addView(menuButton, new LinearLayout.LayoutParams(
            dp(48), dp(48)
        ));

        TextView brand = new TextView(this);
        brand.setText(title);
        brand.setTextColor(primary);
        brand.setTextSize(15);
        brand.setTypeface(null, Typeface.BOLD);
        brand.setLetterSpacing(0.12f);

        LinearLayout.LayoutParams brandParams =
            new LinearLayout.LayoutParams(0, -2, 1);

        brandParams.setMargins(dp(12), 0, 0, 0);
        toolbar.addView(brand, brandParams);

        menuButton.setOnClickListener(v -> toggleDrawer());

        FrameLayout searchButton = createIconButton(false);

        toolbar.addView(searchButton, new LinearLayout.LayoutParams(
            dp(48), dp(48)
        ));

        searchButton.setOnClickListener(v -> toggleSearch());

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setHint("Search your archive");
        searchInput.setHintTextColor(secondary);
        searchInput.setTextColor(primary);
        searchInput.setTextSize(15);
        searchInput.setPadding(dp(16), 0, dp(16), 0);
        searchInput.setBackground(background(card, 12));
        searchInput.setVisibility(View.GONE);

        LinearLayout.LayoutParams searchParams =
            new LinearLayout.LayoutParams(-1, dp(50));

        searchParams.setMargins(dp(22), dp(8), dp(22), dp(8));
        main.addView(searchInput, searchParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        main.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(26), dp(30), dp(26), dp(40));

        scroll.addView(content);

        createDrawer();
        searchInput.addTextChangedListener(new TextWatcher() {

    @Override
    public void beforeTextChanged(
        CharSequence s, int start, int count, int after
    ) {}

    @Override
    public void onTextChanged(
        CharSequence s, int start, int before, int count
    ) {
        renderSearchResults(s.toString());
    }

    @Override
    public void afterTextChanged(Editable s) {}
});

searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        setContentView(root);
    }

    private void buildHome() {

        currentScreen = "HOME";
        currentUniverseId = -1;

        drawerOpen = false;
        searchOpen = false;

        buildBase("NOVEL ARCHIVE");

        renderHome();
    }

    private void renderHome() {

        content.removeAllViews();

        TextView heading = new TextView(this);
        heading.setText("Your Library");
        heading.setTextColor(primary);
        heading.setTextSize(30);
        heading.setTypeface(null, Typeface.BOLD);

        content.addView(heading);

        TextView subtitle = new TextView(this);
        subtitle.setText("All your fictional worlds in one place.");
        subtitle.setTextColor(secondary);
        subtitle.setTextSize(14);

        LinearLayout.LayoutParams subtitleParams =
            new LinearLayout.LayoutParams(-1, -2);

        subtitleParams.setMargins(0, dp(9), 0, dp(32));
        content.addView(subtitle, subtitleParams);

        // Recently updated novels
        addSection(content, "RECENTLY UPDATED NOVELS");

        List<Novel> recentNovels = getRecentNovels();

        if (recentNovels.isEmpty()) {

            addEmptyCard(
                content,
                "No novels created yet"
            );

        } else {

            int limit = Math.min(3, recentNovels.size());

            for (int i = 0; i < limit; i++) {
                addRecentNovelCard(recentNovels.get(i));
            }
        }

        // Universe list
        LinearLayout sectionHeader = new LinearLayout(this);
        sectionHeader.setOrientation(LinearLayout.HORIZONTAL);
        sectionHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView universeTitle = new TextView(this);
        universeTitle.setText("YOUR UNIVERSES");
        universeTitle.setTextColor(accent);
        universeTitle.setTextSize(12);
        universeTitle.setTypeface(null, Typeface.BOLD);
        universeTitle.setLetterSpacing(0.13f);

        sectionHeader.addView(universeTitle,
            new LinearLayout.LayoutParams(0, -2, 1));

        TextView addButton = createSmallButton("+ CREATE");

        sectionHeader.addView(addButton);

        content.addView(sectionHeader);

        addButton.setOnClickListener(v -> showUniverseDialog(null));

        List<Universe> universes = repository.getAllUniverses();

        if (universes.isEmpty()) {

            addEmptyCard(
                content,
                "No universes created yet"
            );

        } else {

            for (Universe universe : universes) {
                addUniverseCard(universe);
            }
        }
    }

    // ==================================================
    // HOME CARDS
    // ==================================================

    private void addRecentNovelCard(Novel novel) {

        LinearLayout box = createCard();

        LinearLayout.LayoutParams params = sectionCardMargins();
        params.bottomMargin = dp(10);

        content.addView(box, params);

        TextView title = new TextView(this);
        title.setText(novel.title);
        title.setTextColor(primary);
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);

        box.addView(title);

        TextView description = new TextView(this);

        Universe universe =
            repository.getUniverseById(novel.universeId);

        description.setText(
            universe == null
                ? "Universe unavailable"
                : universe.name
        );

        description.setTextColor(secondary);
        description.setTextSize(12);

        LinearLayout.LayoutParams descParams =
            new LinearLayout.LayoutParams(-1, -2);

        descParams.topMargin = dp(6);
        box.addView(description, descParams);

        box.setOnClickListener(v -> {
            if (universe != null) {
                openUniverse(universe.id);
            }
        });
    }

    private void addUniverseCard(Universe universe) {

        LinearLayout box = createCard();

        content.addView(box, sectionCardMargins());

        TextView title = new TextView(this);
        title.setText(universe.name);
        title.setTextColor(primary);
        title.setTextSize(19);
        title.setTypeface(null, Typeface.BOLD);

        box.addView(title);

        if (universe.description != null) {

            TextView description = new TextView(this);
            description.setText(universe.description);
            description.setTextColor(secondary);
            description.setTextSize(13);

            LinearLayout.LayoutParams descParams =
                new LinearLayout.LayoutParams(-1, -2);

            descParams.topMargin = dp(7);
            box.addView(description, descParams);
        }

        int novelCount =
            repository.getNovelsForUniverse(universe.id).size();

        TextView count = new TextView(this);
        count.setText(
            novelCount + (novelCount == 1 ? " novel" : " novels")
        );
        count.setTextColor(accent);
        count.setTextSize(12);

        LinearLayout.LayoutParams countParams =
            new LinearLayout.LayoutParams(-1, -2);

        countParams.topMargin = dp(12);
        box.addView(count, countParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams actionParams =
            new LinearLayout.LayoutParams(-1, -2);

        actionParams.topMargin = dp(15);
        box.addView(actions, actionParams);

        TextView edit = createSmallButton("EDIT");
        TextView delete = createSmallButton("DELETE");
        TextView open = createSmallButton("OPEN");

        actions.addView(edit);
        actions.addView(delete);

        LinearLayout.LayoutParams openParams =
            new LinearLayout.LayoutParams(-2, -2);

        openParams.leftMargin = dp(8);
        actions.addView(open, openParams);

        box.setOnClickListener(v -> openUniverse(universe.id));

        edit.setOnClickListener(v ->
            showUniverseDialog(universe)
        );

        delete.setOnClickListener(v ->
            confirmDeleteUniverse(universe)
        );

        open.setOnClickListener(v ->
            openUniverse(universe.id)
        );
    }

    private void addEmptyCard(
        LinearLayout parent,
        String message
    ) {

        LinearLayout box = createCard();

        parent.addView(box, sectionCardMargins());

        TextView text = new TextView(this);
        text.setText(message);
        text.setTextColor(secondary);
        text.setTextSize(14);

        box.addView(text);
    }

    // ==================================================
    // UNIVERSE SCREEN
    // ==================================================

    private void openUniverse(long universeId) {

        Universe universe =
            repository.getUniverseById(universeId);

        if (universe == null) {
            showMessage("This universe no longer exists.");
            buildHome();
            return;
        }

        currentScreen = "UNIVERSE";
        currentUniverseId = universeId;

        drawerOpen = false;
        searchOpen = false;

        buildBase("NOVEL ARCHIVE");

        renderUniverse(universe);
    }

    private void renderUniverse(Universe universe) {

        content.removeAllViews();

        TextView back = createTextButton("←  BACK TO LIBRARY");
        content.addView(back);

        back.setOnClickListener(v -> buildHome());

        TextView heading = new TextView(this);
        heading.setText(universe.name);
        heading.setTextColor(primary);
        heading.setTextSize(29);
        heading.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams headingParams =
            new LinearLayout.LayoutParams(-1, -2);

        headingParams.topMargin = dp(25);
        content.addView(heading, headingParams);

        if (universe.description != null) {

            TextView description = new TextView(this);
            description.setText(universe.description);
            description.setTextColor(secondary);
            description.setTextSize(14);

            LinearLayout.LayoutParams descParams =
                new LinearLayout.LayoutParams(-1, -2);

            descParams.topMargin = dp(9);
            descParams.bottomMargin = dp(24);

            content.addView(description, descParams);
        }

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView novelsTitle = new TextView(this);
        novelsTitle.setText("NOVELS");
        novelsTitle.setTextColor(accent);
        novelsTitle.setTextSize(12);
        novelsTitle.setTypeface(null, Typeface.BOLD);
        novelsTitle.setLetterSpacing(0.13f);

        header.addView(novelsTitle,
            new LinearLayout.LayoutParams(0, -2, 1));

        TextView addNovel = createSmallButton("+ ADD NOVEL");
        header.addView(addNovel);

        content.addView(header);

        addNovel.setOnClickListener(v -> showNovelDialog(universe.id, null));

        List<Novel> novels =
            repository.getNovelsForUniverse(universe.id);

        if (novels.isEmpty()) {

            addEmptyCard(content, "No novels in this universe yet.");

        } else {

            for (Novel novel : novels) {
                addNovelCard(novel);
            }
        }

        // Universe management
        addSection(content, "UNIVERSE SETTINGS");

        LinearLayout settings = createCard();

        content.addView(settings, sectionCardMargins());

        TextView edit = createTextButton("Edit universe details");
        settings.addView(edit);

        edit.setOnClickListener(v ->
            showUniverseDialog(universe)
        );

        TextView delete = createTextButton("Delete universe");
        delete.setTextColor(danger);

        LinearLayout.LayoutParams deleteParams =
            new LinearLayout.LayoutParams(-1, -2);

        deleteParams.topMargin = dp(14);
        settings.addView(delete, deleteParams);

        delete.setOnClickListener(v ->
            confirmDeleteUniverse(universe)
        );
    }

    // ==================================================
    // NOVEL CARDS
    // ==================================================

    private void addNovelCard(Novel novel) {

        LinearLayout box = createCard();

        content.addView(box, sectionCardMargins());

        TextView title = new TextView(this);
        title.setText(novel.title);
        title.setTextColor(primary);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);

        box.addView(title);

        if (novel.description != null) {

            TextView description = new TextView(this);
            description.setText(novel.description);
            description.setTextColor(secondary);
            description.setTextSize(13);

            LinearLayout.LayoutParams descParams =
                new LinearLayout.LayoutParams(-1, -2);

            descParams.topMargin = dp(7);
            box.addView(description, descParams);
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams actionParams =
            new LinearLayout.LayoutParams(-1, -2);

        actionParams.topMargin = dp(14);
        box.addView(actions, actionParams);

        TextView edit = createSmallButton("EDIT");
        TextView delete = createSmallButton("DELETE");

        actions.addView(edit);
        actions.addView(delete);

        edit.setOnClickListener(v ->
            showNovelDialog(novel.universeId, novel)
        );

        delete.setOnClickListener(v ->
            confirmDeleteNovel(novel)
        );
    }

    // ==================================================
    // CREATE / EDIT DIALOGS
    // ==================================================

    private void showUniverseDialog(Universe existing) {

        boolean editing = existing != null;

        LinearLayout form = createDialogForm();

        EditText nameInput = createFormInput("Universe name");
        EditText descriptionInput =
            createFormInput("Description (optional)");

        descriptionInput.setMinLines(3);
        descriptionInput.setGravity(Gravity.TOP);

        if (editing) {
            nameInput.setText(existing.name);

            if (existing.description != null) {
                descriptionInput.setText(existing.description);
            }
        }

        form.addView(nameInput);
        form.addView(descriptionInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(editing ? "Edit Universe" : "Create Universe")
            .setView(form)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton(editing ? "SAVE" : "CREATE", null)
            .create();

        dialog.setOnShowListener(v -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {

                    String name = nameInput.getText().toString().trim();
                    String description =
                        descriptionInput.getText().toString();

                    if (name.isEmpty()) {
                        nameInput.setError("Enter a universe name");
                        return;
                    }

                    try {

                        if (editing) {

                            repository.updateUniverse(
                                existing.id,
                                name,
                                description
                            );

                            showMessage("Universe updated.");

                        } else {

                            repository.createUniverse(
                                name,
                                description
                            );

                            showMessage("Universe created.");
                        }

                        dialog.dismiss();

                        if (currentScreen.equals("UNIVERSE")) {
                            refreshCurrentUniverse();
                        } else {
                            buildHome();
                        }

                    } catch (Exception e) {
                        showMessage("Unable to save universe: " + e.getMessage());
                    }
                });
        });

        dialog.show();
    }

    private void showNovelDialog(
        long universeId,
        Novel existing
    ) {

        boolean editing = existing != null;

        LinearLayout form = createDialogForm();

        EditText titleInput = createFormInput("Novel title");
        EditText descriptionInput =
            createFormInput("Description (optional)");

        descriptionInput.setMinLines(3);
        descriptionInput.setGravity(Gravity.TOP);

        if (editing) {
            titleInput.setText(existing.title);

            if (existing.description != null) {
                descriptionInput.setText(existing.description);
            }
        }

        form.addView(titleInput);
        form.addView(descriptionInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(editing ? "Edit Novel" : "Create Novel")
            .setView(form)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton(editing ? "SAVE" : "CREATE", null)
            .create();

        dialog.setOnShowListener(v -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {

                    String title =
                        titleInput.getText().toString().trim();

                    String description =
                        descriptionInput.getText().toString();

                    if (title.isEmpty()) {
                        titleInput.setError("Enter a novel title");
                        return;
                    }

                    try {

                        if (editing) {

                            repository.updateNovel(
                                existing.id,
                                title,
                                description
                            );

                            showMessage("Novel updated.");

                        } else {

                            repository.createNovel(
                                universeId,
                                title,
                                description
                            );

                            showMessage("Novel created.");
                        }

                        dialog.dismiss();

                        if (currentScreen.equals("UNIVERSE")) {
                            refreshCurrentUniverse();
                        } else {
                            buildHome();
                        }

                    } catch (Exception e) {
                        showMessage("Unable to save novel: " + e.getMessage());
                    }
                });
        });

        dialog.show();
    }

    // ==================================================
    // DELETE CONFIRMATIONS
    // ==================================================

    private void confirmDeleteUniverse(Universe universe) {

        new AlertDialog.Builder(this)
            .setTitle("Delete Universe?")
            .setMessage(
                "Delete \"" + universe.name + "\"?\n\n" +
                "A universe containing novels cannot be deleted. " +
                "Delete its novels individually first."
            )
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("DELETE", (dialog, which) -> {

                boolean deleted =
                    repository.deleteUniverse(universe.id);

                if (deleted) {
                    showMessage("Universe deleted.");
                    buildHome();
                } else {
                    showMessage(
                        "Cannot delete this universe. " +
                        "It may still contain novels."
                    );
                }
            })
            .show();
    }

    private void confirmDeleteNovel(Novel novel) {

        new AlertDialog.Builder(this)
            .setTitle("Delete Novel?")
            .setMessage(
                "Delete \"" + novel.title + "\"?\n\n" +
                "Its associated characters, timeline events, " +
                "calendar and other dependent records will also be deleted."
            )
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("DELETE", (dialog, which) -> {

                try {

                    boolean deleted =
                        repository.deleteNovel(novel.id);

                    if (deleted) {
                        showMessage("Novel deleted.");
                        refreshCurrentUniverse();
                    } else {
                        showMessage("Novel could not be found.");
                    }

                } catch (Exception e) {
                    showMessage("Unable to delete novel: " + e.getMessage());
                }
            })
            .show();
    }

    // ==================================================
    // RECENT NOVELS
    // ==================================================

    private List<Novel> getRecentNovels() {

        List<Novel> all = new ArrayList<>();

        List<Universe> universes =
            repository.getAllUniverses();

        for (Universe universe : universes) {
            all.addAll(
                repository.getNovelsForUniverse(universe.id)
            );
        }

        Collections.sort(all, (a, b) ->
            Long.compare(b.updatedAt, a.updatedAt)
        );

        return all;
    }

    // ==================================================
    // DRAWER
    // ==================================================

    private void createDrawer() {

    scrim = new View(this);
    scrim.setBackgroundColor(0x99000000);
    scrim.setVisibility(View.GONE);

    root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));

    scrim.setOnClickListener(v -> closeDrawer());

    drawer = new LinearLayout(this);
    drawer.setOrientation(LinearLayout.VERTICAL);
    drawer.setPadding(dp(22), dp(42), dp(18), dp(24));
    drawer.setBackgroundColor(card);
    drawer.setVisibility(View.GONE);

    FrameLayout.LayoutParams drawerParams =
        new FrameLayout.LayoutParams(dp(300), -1, Gravity.START);

    root.addView(drawer, drawerParams);

    TextView title = new TextView(this);
    title.setText("NOVEL ARCHIVE");
    title.setTextColor(accent);
    title.setTextSize(15);
    title.setTypeface(null, Typeface.BOLD);
    title.setLetterSpacing(0.14f);

    drawer.addView(title);

    TextView subtitle = new TextView(this);
    subtitle.setText("YOUR WRITING WORKSPACE");
    subtitle.setTextColor(secondary);
    subtitle.setTextSize(10);

    LinearLayout.LayoutParams subParams =
        new LinearLayout.LayoutParams(-1, -2);

    subParams.setMargins(0, dp(9), 0, dp(30));
    drawer.addView(subtitle, subParams);

    addMenuItem(drawer, "Library", currentScreen.equals("HOME"));

    if (currentScreen.equals("UNIVERSE")) {

        addDivider(drawer);

        TextView current = new TextView(this);
        current.setText("CURRENT UNIVERSE");
        current.setTextColor(secondary);
        current.setTextSize(11);
        current.setPadding(dp(15), dp(16), dp(12), dp(8));

        drawer.addView(current);

        Universe universe =
            repository.getUniverseById(currentUniverseId);

        if (universe != null) {
            addMenuItem(drawer, universe.name, true);
        }
    }
}
    
    private void addMenuItem(
        LinearLayout parent,
        String label,
        boolean selected
    ) {

        TextView item = new TextView(this);
        item.setText(label);
        item.setTextColor(primary);
        item.setTextSize(16);
        item.setTypeface(null, Typeface.BOLD);
        item.setPadding(dp(15), dp(17), dp(12), dp(17));

        if (selected) {
            item.setBackground(background(bg, 11));
        }

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, -2);

        params.setMargins(0, dp(4), 0, dp(4));
        parent.addView(item, params);

        item.setOnClickListener(v -> {
            closeDrawer();
            buildHome();
        });
    }

    private void addDivider(LinearLayout parent) {

        View line = new View(this);
        line.setBackgroundColor(0x334A5266);

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, dp(1));

        params.setMargins(0, dp(13), 0, dp(13));
        parent.addView(line, params);
    }

    private void toggleDrawer() {

        if (drawerOpen) {
            closeDrawer();
            return;
        }

        scrim.setVisibility(View.VISIBLE);
        drawer.setVisibility(View.VISIBLE);

        scrim.bringToFront();
        drawer.bringToFront();

        drawerOpen = true;
    }

    private void closeDrawer() {

        if (drawer == null) return;

        drawer.setVisibility(View.GONE);
        scrim.setVisibility(View.GONE);

        drawerOpen = false;
    }

    // ==================================================
    // SEARCH VISIBILITY
    // ==================================================

    private void toggleSearch() {

    searchOpen = !searchOpen;

    searchInput.setVisibility(
        searchOpen ? View.VISIBLE : View.GONE
    );

    if (searchOpen) {

        searchInput.requestFocus();
        renderSearchResults(searchInput.getText().toString());

    } else {

        searchInput.setText("");
        showingSearchResults = false;

        if (currentScreen.equals("UNIVERSE")) {
            refreshCurrentUniverse();
        } else {
            buildHome();
        }

        InputMethodManager imm =
            (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE
            );

        if (imm != null) {
            imm.hideSoftInputFromWindow(
                searchInput.getWindowToken(), 0
            );
        }
    }
}

    // ==================================================
    // UI HELPERS
    // ==================================================

    private void addSection(
        LinearLayout parent,
        String label
    ) {

        TextView section = new TextView(this);
        section.setText(label);
        section.setTextColor(accent);
        section.setTextSize(12);
        section.setTypeface(null, Typeface.BOLD);
        section.setLetterSpacing(0.13f);

        parent.addView(section);
    }

    private LinearLayout createCard() {

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setMinimumHeight(dp(75));
        box.setPadding(dp(20), dp(20), dp(20), dp(20));
        box.setBackground(background(card, 16));

        return box;
    }

    private LinearLayout.LayoutParams sectionCardMargins() {

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, -2);

        params.setMargins(0, dp(15), 0, dp(28));

        return params;
    }

    private TextView createSmallButton(String text) {

        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(accent);
        button.setTextSize(11);
        button.setTypeface(null, Typeface.BOLD);
        button.setPadding(dp(10), dp(9), dp(10), dp(9));
        button.setBackground(background(bg, 9));
        button.setGravity(Gravity.CENTER);

        return button;
    }

    private TextView createTextButton(String text) {

        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(accent);
        button.setTextSize(14);
        button.setPadding(0, dp(10), 0, dp(10));

        return button;
    }

    private LinearLayout createDialogForm() {

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(22), dp(10), dp(22), dp(5));

        return form;
    }

    private EditText createFormInput(String hint) {

        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(secondary);
        input.setTextColor(primary);
        input.setTextSize(15);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        input.setBackground(background(card, 10));

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, -2);

        params.bottomMargin = dp(13);

        input.setLayoutParams(params);

        return input;
    }

    private FrameLayout createIconButton(boolean hamburger) {

        FrameLayout button = new FrameLayout(this);
        button.setBackground(background(card, 13));
        button.setClickable(true);
        button.setFocusable(true);

        IconView icon = new IconView(this, hamburger);

        FrameLayout.LayoutParams iconParams =
            new FrameLayout.LayoutParams(
                dp(24), dp(24), Gravity.CENTER
            );

        button.addView(icon, iconParams);

        button.setContentDescription(
            hamburger ? "Open navigation menu" : "Open search"
        );

        return button;
    }

    private GradientDrawable background(
        int color,
        int radius
    ) {

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));

        return shape;
    }

    private int dp(int value) {

        return (int) (
            value * getResources().getDisplayMetrics().density + 0.5f
        );
    }

    private void showMessage(String message) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show();
    }

    private void refreshCurrentUniverse() {

        Universe universe =
            repository.getUniverseById(currentUniverseId);

        if (universe == null) {
            buildHome();
        } else {
            openUniverse(universe.id);
        }
    }

    // ==================================================
    // CUSTOM ICON DRAWING
    // ==================================================

    private class IconView extends View {

        private final Paint paint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

        private final boolean hamburger;

        IconView(Activity context, boolean hamburger) {
            super(context);

            this.hamburger = hamburger;

            paint.setColor(primary);
            paint.setStrokeWidth(dp(2));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float w = getWidth();
            float h = getHeight();

            if (hamburger) {

                canvas.drawLine(
                    w * 0.18f, h * 0.28f,
                    w * 0.82f, h * 0.28f, paint
                );

                canvas.drawLine(
                    w * 0.18f, h * 0.50f,
                    w * 0.82f, h * 0.50f, paint
                );

                canvas.drawLine(
                    w * 0.18f, h * 0.72f,
                    w * 0.82f, h * 0.72f, paint
                );

            } else {

                canvas.drawCircle(
                    w * 0.43f, h * 0.42f,
                    w * 0.25f, paint
                );

                canvas.drawLine(
                    w * 0.61f, h * 0.61f,
                    w * 0.85f, h * 0.85f,
                    paint
                );
            }
        }
    }

    // ==================================================
    // BACK NAVIGATION
    // ==================================================

    @Override
    public void onBackPressed() {

        if (drawerOpen) {
            closeDrawer();

        } else if (searchOpen) {
            toggleSearch();

        } else if (currentScreen.equals("UNIVERSE")) {
            buildHome();

        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (repository != null) {
            repository.close();
        }
        if (searchRepository != null) {
            searchRepository.close();
    }
}

private void renderSearchResults(String query) {

    if (!searchOpen) return;

    showingSearchResults = true;
    content.removeAllViews();

    TextView heading = new TextView(this);
    heading.setText("SEARCH RESULTS");
    heading.setTextColor(accent);
    heading.setTextSize(12);
    heading.setTypeface(null, Typeface.BOLD);
    heading.setLetterSpacing(0.13f);

    content.addView(heading);

    if (query.trim().isEmpty()) {

        addEmptyCard(content, "Start typing to search your archive.");

        return;
    }

    List<SearchResult> results =
        searchRepository.search(query);

    if (results.isEmpty()) {

        addEmptyCard(content, "No matching results found.");

        return;
    }

    TextView count = new TextView(this);
    count.setText(results.size() + " matching results");
    count.setTextColor(secondary);
    count.setTextSize(13);

    LinearLayout.LayoutParams countParams =
        new LinearLayout.LayoutParams(-1, -2);

    countParams.topMargin = dp(9);
    content.addView(count, countParams);

    for (SearchResult result : results) {

        LinearLayout box = createCard();

        content.addView(box, sectionCardMargins());

        TextView type = new TextView(this);
        type.setText(
            result.type.equals(ArchiveSearchRepository.TYPE_UNIVERSE)
                ? "UNIVERSE"
                : "NOVEL"
        );
        type.setTextColor(accent);
        type.setTextSize(10);
        type.setTypeface(null, Typeface.BOLD);
        type.setLetterSpacing(0.12f);

        box.addView(type);

        TextView title = new TextView(this);
        title.setText(result.title);
        title.setTextColor(primary);
        title.setTextSize(17);
        title.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams titleParams =
            new LinearLayout.LayoutParams(-1, -2);

        titleParams.topMargin = dp(7);
        box.addView(title, titleParams);

        TextView parent = new TextView(this);
        parent.setText(result.parentTitle);
        parent.setTextColor(secondary);
        parent.setTextSize(12);

        LinearLayout.LayoutParams parentParams =
            new LinearLayout.LayoutParams(-1, -2);

        parentParams.topMargin = dp(5);
        box.addView(parent, parentParams);

        if (result.description != null
            && !result.description.trim().isEmpty()) {

            TextView description = new TextView(this);
            description.setText(result.description);
            description.setTextColor(secondary);
            description.setTextSize(13);
            description.setMaxLines(3);

            LinearLayout.LayoutParams descParams =
                new LinearLayout.LayoutParams(-1, -2);

            descParams.topMargin = dp(8);
            box.addView(description, descParams);
        }

        box.setOnClickListener(v -> {

            if (result.type.equals(
                ArchiveSearchRepository.TYPE_UNIVERSE
            )) {
                openUniverse(result.universeId);

            } else {
                openUniverse(result.universeId);
            }
        });
    }
}
}