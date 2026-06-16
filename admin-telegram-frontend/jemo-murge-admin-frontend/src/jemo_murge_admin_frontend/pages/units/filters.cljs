(ns jemo-murge-admin-frontend.pages.units.filters
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   )
  )



(defn transform_filters [m]
  (->> m
       (filter (fn [[_ v]] (not-empty v)))
       (mapv (fn [[k v]] {:attribute_name (name k) :attribute_values v}))))



(defn filters []
  (let [Select antd/Select
        filters (reagent/cursor app-state [:filters])
        filters_picked (reagent/cursor app-state [:filters_picked])
        search_value (reagent/cursor app-state [:search_value])]
    (fn []
      (let [find-filter (fn [names]
                          (first (filter (fn [item]
                                           (some #(= % (:attribute_name item)) names))
                                         @filters)))
            type-data (find-filter ["product_type" "type"])
            section-data (find-filter ["section" "categories"])
            collection-data (find-filter ["collection"])]
        [:div {:style {:background "#D3EAFF"
                       :border-radius 15
                       :height 50
                       :overflow "hidden"
                       :flex-wrap "nowrap"
                       :font-size 24
                       :display "flex"
                       :justify-content "space-around"
                       :align-items "center"
                       :box-shadow "0 2px 8px rgba(0, 4, 6, 0.25)"}
               }
         [:> Select {:style {:background "transparent"
                             :border "none"
                             :box-shadow "none"
                             :min-width "200px"}
                     :dropdownStyle {:background "#D3EAFF"}
                     :placeholder (or (:attribute_name_rus type-data) "Тип товара")
                     :bordered false
                     :value (:product_type @filters_picked)
                     :mode "multiple"
                     :allowClear true
                     :options (map (fn [v] {:label v, :value v})
                                   (:attribute_values type-data))
                     :onChange (fn [values]
                                 (swap! app-state
                                        assoc-in
                                        [:filters_picked :product_type]
                                        (js->clj values)))
                     }
          ]
         
         [:> Select {:style {:background "transparent"
                             :border "none"
                             :box-shadow "none"
                             :min-width "200px"}
                     :dropdownStyle {:background "#D3EAFF"}
                     :placeholder (or (:attribute_name_rus section-data) "Раздел")
                     :bordered false
                     :mode "multiple"
                     :value (:section @filters_picked)
                     :allowClear true
                     :options (map (fn [v] {:label v, :value v})
                                   (:attribute_values section-data))
                     :onChange (fn [values]
                                 (swap! app-state
                                        assoc-in
                                        [:filters_picked :section]
                                        (js->clj values)))
                     }
          ]
         
         [:> Select {:style {:background "transparent"
                             :border "none"
                             :box-shadow "none"
                             :min-width "200px"}
                     :dropdownStyle {:background "#D3EAFF"}
                     :placeholder (or (:attribute_name_rus collection-data) "Коллекция")
                     :bordered false
                     :value (:collection @filters_picked)
                     :mode "tags"
                     :allowClear true
                     :options (map (fn [v] {:label v, :value v})
                                   (:attribute_values collection-data))
                     :onChange (fn [values]
                                 (swap! app-state
                                        assoc-in
                                        [:filters_picked :collection]
                                        (js->clj values)))
                     }
          ]
         
         [:> Select {:style {:background "transparent"
                             :border "none"
                             :box-shadow "none"}
                     :dropdownStyle {:background "#D3EAFF"}
                     :placeholder "Сортировка"
                     :bordered false
                     :allowClear true
                     }
          ]
         ]
         )
         )
    )
    )
 
