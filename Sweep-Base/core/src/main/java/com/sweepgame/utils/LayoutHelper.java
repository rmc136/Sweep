package com.sweepgame.utils;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LayoutHelper {
    private static final Logger logger = LoggerFactory.getLogger(LayoutHelper.class);

    private static LayoutHelper instance;
    private boolean isMobile;
    private float viewportWidth;
    private float viewportHeight;

    private LayoutHelper() {
        // Detect platform
        isMobile = Gdx.app.getType() == Application.ApplicationType.Android || 
                   Gdx.app.getType() == Application.ApplicationType.iOS;

        // Set base viewport dimensions
        if (isMobile) {
            viewportWidth = 720f;
            viewportHeight = 1280f;
            logger.info("LayoutHelper initialized for MOBILE: {}x{}", viewportWidth, viewportHeight);
        } else {
            viewportWidth = 1280f;
            viewportHeight = 720f;
            logger.info("LayoutHelper initialized for DESKTOP: {}x{}", viewportWidth, viewportHeight);
        }
    }

    public static LayoutHelper getInstance() {
        if (instance == null) {
            instance = new LayoutHelper();
        }
        return instance;
    }

    public boolean isMobile() {
        return isMobile;
    }

    public float getViewportWidth() {
        return viewportWidth;
    }

    public float getViewportHeight() {
        return viewportHeight;
    }

    // --- Percentage-based Sizing ---

    public float getWidth(float percent) {
        return viewportWidth * percent;
    }

    public float getHeight(float percent) {
        return viewportHeight * percent;
    }

    // --- Standard Component Sizes (Percentages) ---

    // Hand Cards
    public float getHandCardWidth() {
        return isMobile ? getWidth(0.14f) : 80f; // Mobile: ~100px (14% of 720), Desktop: 80px
    }

    public float getHandCardHeight() {
        return isMobile ? getHeight(0.10f) : 100f; // Mobile: ~130px (10% of 1280), Desktop: 100px
    }

    // Table Cards
    public float getTableCardWidth() {
        return isMobile ? getWidth(0.21f) : 125f; // Mobile: ~150px (21% of 720), Desktop: 125px
    }

    public float getTableCardHeight() {
        return isMobile ? getHeight(0.14f) : 150f; // Mobile: ~180px (14% of 1280), Desktop: 150px
    }
    
    // Animation Cards
    public float getAnimCardWidth() {
         return isMobile ? getWidth(0.21f) : 120f;
    }

    public float getAnimCardHeight() {
        return isMobile ? getHeight(0.14f) : 140f;
    }

    // --- Padding & Spacing ---
    
    public float getStandardPadding() {
        return isMobile ? 5f : 2f;
    }

    public float getSeatWidth() {
        return isMobile ? getWidth(0.45f) : getWidth(0.25f);
    }
    
    // --- Scaling for UI Elements ---
    
    /**
     * Scale a value based on platform (mobile vs desktop)
     * For desktop, returns the value as-is
     * For mobile, scales proportionally
     */
    public float scale(float value) {
        if (isMobile) {
            // Scale based on mobile viewport (720x1280)
            return value * (viewportWidth / 1280f);
        }
        return value;
    }
}
