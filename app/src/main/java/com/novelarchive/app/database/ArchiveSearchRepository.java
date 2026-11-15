
package com.novelarchive.app.database;

import android.content.Context;

import com.novelarchive.app.database.UniverseRepository.Novel;
import com.novelarchive.app.database.UniverseRepository.Universe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ArchiveSearchRepository {

    public static final String TYPE_UNIVERSE = "UNIVERSE";
    public static final String TYPE_NOVEL = "NOVEL";

    public static class SearchResult {
        public final String type;
        public final long id;
        public final long universeId;
        public final String title;
        public final String description;
        public final String parentTitle;

        public SearchResult(
            String type,
            long id,
            long universeId,
            String title,
            String description,
            String parentTitle
        ) {
            this.type = type;
            this.id = id;
            this.universeId = universeId;
            this.title = title;
            this.description = description;
            this.parentTitle = parentTitle;
        }
    }

    private final UniverseRepository repository;

    public ArchiveSearchRepository(Context context) {
        repository = new UniverseRepository(context);
    }

    public List<SearchResult> search(String query) {

        List<SearchResult> results = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        String term = query.trim().toLowerCase(Locale.ROOT);

        List<Universe> universes = repository.getAllUniverses();

        for (Universe universe : universes) {

            if (matches(universe.name, term)
                || matches(universe.description, term)) {

                results.add(new SearchResult(
                    TYPE_UNIVERSE,
                    universe.id,
                    universe.id,
                    universe.name,
                    universe.description,
                    "Universe"
                ));
            }

            List<Novel> novels =
                repository.getNovelsForUniverse(universe.id);

            for (Novel novel : novels) {

                if (matches(novel.title, term)
                    || matches(novel.description, term)) {

                    results.add(new SearchResult(
                        TYPE_NOVEL,
                        novel.id,
                        universe.id,
                        novel.title,
                        novel.description,
                        universe.name
                    ));
                }
            }
        }

        return results;
    }

    private boolean matches(String value, String term) {

        return value != null
            && value.toLowerCase(Locale.ROOT).contains(term);
    }

    public void close() {
        repository.close();
    }
}
