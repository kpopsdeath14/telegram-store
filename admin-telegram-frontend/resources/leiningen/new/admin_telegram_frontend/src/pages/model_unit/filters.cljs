(ns {{name}}.pages.model-unit.filters
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   )
  )



(defn filters []
  (let [Select antd/Select
        
        filters (reagent/cursor app-state [:filters])
        filters_picked (reagent/cursor app-state [:filters_picked]) 
        products_mode (reagent/cursor app-state [:products_mode])
        search_value (reagent/cursor app-state [:model_unit_search_value])
        ]
    (fn []
      [:div {:style {:background "#D3EAFF"
                     :border-radius 15
                     :height 50
                     :font-size 24
                     :font-weight 300 
                     :display "flex"
                     :overflow "hidden"
                     :flex-wrap "nowrap"
                     :justify-content "space-around"
                     :box-shadow "0 2px 8px rgba(0, 4, 6, 0.25)"
                     :align-items "center"}}
       [:> Select {:style {:background "transparent"
                           :border "none"
                           :box-shadow "none"
                           :min-width "200px" 
                           }
                   :dropdownStyle {:background "#D3EAFF"}
                   :placeholder "Цвет"
                   :bordered false
                   :value (:color @filters_picked)
                   :mode "multiple"
                   :allowClear true 
                   :options (let [type-data (first (filter #(= "color" (:attribute_name %)) @filters))]
                              (map (fn [v] {:label v, :value v})
                                   (:attribute_values type-data)))
                   :onChange (fn [values]
                               (swap! app-state
                                      assoc-in
                                      [:filters_picked :color]
                                      (js->clj values)
                                      )
                               
                               )
                   }]

       [:> Select {:style {:background "transparent"
                           :border "none"
                           :box-shadow "none"
                           :min-width "200px"}
                   :dropdownStyle {:background "#D3EAFF"}
                   :placeholder "Размер"
                   :bordered false
                   :mode "multiple"
                   :value (:size @filters_picked)
                   :allowClear true
                   :options (let [type-data (first (filter #(= "size" (:attribute_name %)) @filters))]
                              (map (fn [v] {:label v, :value v})
                                   (:attribute_values type-data)))
                   :onChange (fn [values]
                               (swap! app-state
                                      assoc-in
                                      [:filters_picked :size]
                                      (js->clj values))
       
                               
                               )
                   }
                   ]

       [:> Select {:style {:background "transparent"
                           :border "none"
                           :box-shadow "none"
                           :min-width "200px"}
                   :dropdownStyle {:background "#D3EAFF"}
                   :placeholder "Тег"
                   :bordered false
                   :value (:tag @filters_picked)
                   :mode "multiple"
                   :allowClear true
                   :options (let [type-data (first (filter #(= "tag" (:attribute_name %)) @filters))]
                              (map (fn [v] {:label v, :value v})
                                   (:attribute_values type-data)))
                   :onChange (fn [values]
                               (swap! app-state
                                      assoc-in
                                      [:filters_picked :tag]
                                      (js->clj values))
       
                               
                               )
                   }
                   ]

       [:> Select {:style {:background "transparent"
                           :border "none"
                           :box-shadow "none"}
                   :dropdownStyle {:background "#D3EAFF"}
                   :placeholder "Скидка"
                   :bordered false
                   :allowClear true
                   
                   :options (let [type-data (first (filter #(= "sail" (:attribute_name %)) @filters))]
                              (map (fn [v] {:label v, :value v})
                                   (:attribute_values type-data)))
                   :onChange (fn [values]
                               (swap! app-state
                                      assoc-in
                                      [:filters_picked :sail]
                                      (js->clj values))
                   
                               
                               )
                   }]
       
       [:> Select {:style {:background "transparent"
                           :border "none"
                           :box-shadow "none"}
                   :dropdownStyle {:background "#D3EAFF"}
                   :placeholder "Сортировка"
                   :bordered false
                   :allowClear true}]
       ]
       )
       )
       )