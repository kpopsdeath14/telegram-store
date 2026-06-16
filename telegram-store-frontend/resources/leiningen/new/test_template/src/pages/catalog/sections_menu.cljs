(ns {{name}}.pages.catalog.sections-menu
  (:require
   ["antd" :as antd]
   [{{name}}.db :refer [app-state]]
   ["@ant-design/icons" :as icons]
   ["react-photo-view" :as photo_review]
   [reagent.core :as reagent :refer [as-element]] 
   )
  )
(defn menu-item [label selected?]
  [:div {:style {
                 :flex-shrink 0
                 :display "flex"
                 :align-items "center"
                 :justify-content "center"
                 :padding "2px 0"
                 :cursor "pointer"
                 :transition "color 0.2s ease, border-color 0.2s ease, transform 0.2s ease, font-variation-settings 0.2s ease"
                 :font-size "12px"
                 :color (if selected? "#111111" "#8c8c8c")
                 :font-weight (if selected? "500" "400")
                 :font-variation-settings (str "\"wght\" " (if selected? 500 400))
                 :transform (if selected? "translateY(-1px)" "translateY(0)")
                 :border-bottom (if selected? "2px solid #111111" "2px solid transparent")
                 }
         :onClick (fn []
                    (if selected?
                      (swap! app-state assoc-in [:filters_picked :section] [])
                      (swap! app-state assoc-in [:filters_picked :section] [label])
                      ) 
                    )
         }
   label])

(defn scroll-menu []
  (let [filters (reagent/cursor app-state [:filters])
        selected-section (reagent/cursor app-state [:filters_picked :section])]
    (fn []
      (let [section-items (:attribute_values
                           (first (filter (fn [f]
                                            (= (:attribute_name f) "section"))
                                          @filters)))
            
            current-selection (first @selected-section)]
        [:div {:style {:display "flex"
                       :overflow-x "auto"
                       :gap "14px"
                       :scrollbar-width "none"
                       :-ms-overflow-style "none"}}
         (doall
          (map-indexed
           (fn [idx item]
             (let [selected? (= item current-selection)]
               ^{:key idx} [menu-item item selected?]))
           section-items))]))))
