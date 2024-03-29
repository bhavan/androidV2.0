package com.townwizard.android.category;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.townwizard.android.R;
import com.townwizard.android.category.Category.ViewType;
import com.townwizard.android.config.Config;
import com.townwizard.android.config.Constants;
import com.townwizard.android.partner.Partner;

public class CategoriesAdapter extends BaseAdapter {

    private Context context;
        
    private Map<CategorySection, List<Category>> categories = 
            new EnumMap<CategorySection, List<Category>>(CategorySection.class);
    private List<Object> categoryList = new ArrayList<Object>();    
    
    public CategoriesAdapter(Context context) {
        this.context = context;    
    }
    
    public void addItem(Category category) {        
        if(!category.hasView()) return;
        
        CategorySection section = getSection(category);
        List<Category> sectionCategories = categories.get(section);
        if(sectionCategories == null) {
            sectionCategories = new ArrayList<Category>();
            categories.put(section, sectionCategories);
        }
        sectionCategories.add(category);
        
        List<Object> categoryList = new ArrayList<Object>();
        for (Map.Entry<CategorySection, List<Category>> e : categories.entrySet()) {
            categoryList.add(e.getKey());
            for(Category c : e.getValue()) {
                categoryList.add(c);
            }
        }
        
        this.categoryList = categoryList;
        
        notifyDataSetChanged();
    }
    
    @Override
    public int getCount() {
        return categoryList.size();
    }

    @Override
    public Object getItem(int position) {
        return categoryList.get(position-1);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }
    
    @Override
    public int getItemViewType(int position) {
        if(position == 0) {
            return HEADER_VIEW_TYPE;
        }
        Object item = categoryList.get(position);
        if(item instanceof CategorySection) {
            return SECTION_VIEW_TYPE;
        }
        return CATEGORY_VIEW_TYPE;
    }
    
    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) == CATEGORY_VIEW_TYPE;
    }    

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Object item = categoryList.get(position);
        if(item instanceof CategorySection) {
            return getSectionView((CategorySection)item, convertView, parent);
        }
        return getCategoryView((Category)item, convertView, parent);
    }
    
    public Category getAboutUsCategory() {
        return getCategoryByName(Constants.ABOUT_US, Config.DEFAULT_ABOUT_US_URI);
    }
    
    public Category getHomeCategory() {
        Partner p = Config.getConfig(context).getPartner();
        if(p != null) {
            String partnerName = p.getName();
            if(Constants.CONTENT_PARTNER_EVENTS.equals(partnerName)) {
                return getCategoryByName(Constants.EVENTS, null);
            } else if(Constants.CONTENT_PARTNER_RESTAURANTS.equals(partnerName)) {
                return getCategoryByName(Constants.RESTAURANTS, null);
            } else if(Constants.CONTENT_PARTNER_PLACES.equals(partnerName)) {
                return getCategoryByName(Constants.PLACES, null);
            }
        }        
        
        for(String cName : new String[]{Constants.HOME, Constants.NEWS, Constants.EVENTS}) {
            Category c = getCategoryByName(cName, null);
            if(c != null && c.getSectionName() != null) {
                return c;
            }
        }
        return getCategoryByName(Constants.HOME, Config.DEFAULT_HOME_URI);
    }
    
    // bhavan: this method should work using section name, not display name
    private Category getCategoryByName(String name, String defaultUrl) {
        Category category = null;
        List<Category> generalCategories = categories.get(CategorySection.GENERAL);
        if(generalCategories != null) {
            for(Category c : generalCategories) {
                //bhavan: if(name.equals(c.getName())) {
                if(name.equals(c.getSectionName())) {    
                    category = c;
                    break;
                }
            }
        }
        if(category == null) {
            if(defaultUrl != null) {
                // bhavan: create cateory using same name as display name and section name
                // category = new Category(null, name, defaultUrl, ViewType.WEB);
                category = new Category(null, name, name, defaultUrl, ViewType.WEB);
            }
        }       
        return category;
    }
    
    private View getSectionView(CategorySection section, View convertView, ViewGroup parent) {
        View view = convertView;
        if(view == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(R.layout.category_section_header, parent, false);
        }
        TextView textView = (TextView)view.findViewById(R.id.category_section_title);
        textView.setText(section.getName());
        return view;
    }
    
    private View getCategoryView(Category category, View convertView, ViewGroup parent) {
        View view = convertView;
        if(view == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
                view = inflater.inflate(R.layout.category, parent, false);
        }
        ImageView imageView = (ImageView) view.findViewById(R.id.section_image);
        TextView textView = (TextView) view.findViewById(R.id.section_text);        
        imageView.setImageBitmap(category.getImage());
        textView.setText(category.getDisplayName());
        return view;
    }    
    
    private CategorySection getSection(Category category) {
        // bhavan: CategorySection result = CATEGORY_TO_SECTION.get(category.getName());
        CategorySection result = CATEGORY_TO_SECTION.get(category.getDisplayName());
        if(result != null) {
            return result;
        }
        return CategorySection.GENERAL;
    }
    
    private static final int VIEW_TYPE_COUNT = 3;
    private static final int HEADER_VIEW_TYPE = 0;
    private static final int SECTION_VIEW_TYPE = 1;
    private static final int CATEGORY_VIEW_TYPE = 2;
    
    private static final Map<String, CategorySection> CATEGORY_TO_SECTION = 
            new HashMap<String, CategorySection>();
    static {        
        CATEGORY_TO_SECTION.put(Constants.HELP_AND_SUPPORT, CategorySection.HELP);
        CATEGORY_TO_SECTION.put(Constants.ADVERTISE_WITH_US, CategorySection.HELP);
        CATEGORY_TO_SECTION.put(Constants.ABOUT_US, CategorySection.HELP);
        CATEGORY_TO_SECTION.put(Constants.CONTACT_US, CategorySection.HELP);
    }

    private static enum CategorySection {
        GENERAL (Constants.POWERED_BY_TW),
        HELP ("");        
        
        private final String name;
        
        private CategorySection(String name) {            
            this.name = name;
        }        
        
        String getName() { return name; }
    }

}
