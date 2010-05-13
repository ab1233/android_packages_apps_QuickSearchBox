/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.quicksearchbox;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Maintains a list of search sources.
 */
public class SearchableSources implements Sources {

    // set to true to enable the more verbose debug logging for this file
    private static final boolean DBG = false;
    private static final String TAG = "QSB.SearchableSources";

    private final Context mContext;
    private final SearchManager mSearchManager;

    // All suggestion sources, by name.
    private HashMap<String, Source> mSources;

    // The web search source to use.
    private Source mWebSearchSource;

    /**
     *
     * @param context Used for looking up source information etc.
     */
    public SearchableSources(Context context) {
        mContext = context;
        mSearchManager = (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
    }

    public Collection<Source> getSources() {
        return mSources.values();
    }

    public Source getSource(String name) {
        return mSources.get(name);
    }

    public Source getWebSearchSource() {
        return mWebSearchSource;
    }

    /**
     * Updates the list of suggestion sources.
     */
    public void update() {
        if (DBG) Log.d(TAG, "update()");
        mSources = new HashMap<String,Source>();

        addSearchableSources();

        mWebSearchSource = createWebSearchSource();
        addSource(mWebSearchSource);
    }

    private void addSearchableSources() {
        List<SearchableInfo> searchables = mSearchManager.getSearchablesInGlobalSearch();
        if (searchables == null) {
            Log.e(TAG, "getSearchablesInGlobalSearch() returned null");
            return;
        }
        for (SearchableInfo searchable : searchables) {
            SearchableSource source = createSearchableSource(searchable);
            if (source != null) {
                if (DBG) Log.d(TAG, "Created source " + source);
                addSource(source);
            }
        }
    }

    private void addSource(Source source) {
        mSources.put(source.getName(), source);
    }

    private Source createWebSearchSource() {
        ComponentName name = getWebSearchComponent();
        SearchableInfo webSearchable = mSearchManager.getSearchableInfo(name);
        if (webSearchable == null) {
            Log.e(TAG, "Web search source " + name + " is not searchable.");
            return null;
        }
        return createSearchableSource(webSearchable);
    }

    private ComponentName getWebSearchComponent() {
        // Looks for an activity in the current package that handles ACTION_WEB_SEARCH.
        // This indirect method is used to allow easy replacement of the web
        // search activity when extending this package.
        Intent webSearchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
        webSearchIntent.setPackage(mContext.getPackageName());
        PackageManager pm = mContext.getPackageManager();
        return webSearchIntent.resolveActivity(pm);
    }

    private SearchableSource createSearchableSource(SearchableInfo searchable) {
        if (searchable == null) return null;
        try {
            return new SearchableSource(mContext, searchable);
        } catch (NameNotFoundException ex) {
            Log.e(TAG, "Source not found: " + ex);
            return null;
        }
    }
}
