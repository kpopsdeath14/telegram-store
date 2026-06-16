(ns {{name}}.pages.units.units
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [{{name}}.pages.units.settings :refer [settings]]
   [{{name}}.pages.units.search :refer [search]]
   [{{name}}.pages.units.filters :refer [filters]]
   [{{name}}.pages.units.list :refer [units_list]]
   [{{name}}.pages.units.footer :refer [footer]]
   [{{name}}.pages.units.modal-product-add :refer [modal_product_add]]
   [{{name}}.pages.units.orders-history :refer [orders_history]]
   [{{name}}.pages.units.modal-help :refer [modal_help]]
   )
  )


(defn transform_filters [m]
  (->> m
       (filter (fn [[_ v]] (not-empty v)))
       (mapv (fn [[k v]] {:attribute_name (name k) :attribute_values v}))))


(defn units_page []
  (let [Button antd/Button
        Row (.-Row antd)
        Col (.-Col antd)

        products_mode (reagent/cursor app-state [:products_mode])
        search_value (reagent/cursor app-state [:search_value])
        ]
    (fn []
        [:div
         [modal_help]
         [:div {:style {:margin-bottom 141}}
          [modal_product_add]
          [:> Row {:gutter [16 16]
                   :style {:margin-bottom 25}}
           [:> Col {:xs 24 :md 12}
            [:div {:className "orders-history-component"
                   :style {:position "relative"}}
             [orders_history]
             [:> Button {:shape "circle"
                         :style {:position "absolute"
                                 :top 12
                                 :right 12
                                 :zIndex 2}
                         :icon (as-element [:> icons/QuestionCircleOutlined])
                         :onClick (fn []
                                    (swap! app-state assoc :units_help_type "orders")
                                    (swap! app-state assoc :units_help_modal_open? true))}]] 
           ]
           [:> Col {:xs 24 :md 12}
            [:div {:className "settings-component"
                   :style {:position "relative"}}
             [settings]
             [:> Button {:shape "circle"
                         :style {:position "absolute"
                                 :top 12
                                 :right 12
                                 :zIndex 2}
                         :icon (as-element [:> icons/QuestionCircleOutlined])
                         :onClick (fn []
                                    (swap! app-state assoc :units_help_type "settings")
                                    (swap! app-state assoc :units_help_modal_open? true))}]]]
           ]
          
          [:> Row {:gutter 16
                   :style {:margin-bottom 25}
                   }

           [:> Col {:span 18}
            [search]
            ]

           [:> Col {:span 6}
            [:> Button {:type "primary"
                        :className "add-product-button"
                        :block true
                        :style {:height 50
                                :overflow "hidden"
                                :white-space "nowrap"
                                :border-radius 15
                                :backgroundColor "#D3EAFF"
                                :color "black"
                                :font-size 24
                                :font-weight 300
                                :box-shadow "0 2px 8px rgba(0, 4, 6, 0.25)"}
                        :onClick (fn []
                                   (swap! app-state assoc :selected_units_catalog [])
                                   (js/window.scrollTo 0 0)
                                   (set! (.-href (.-location js/window)) "#/new-unit"))}
             "Добавить товар"]]]

          [:> Row {:gutter 16
                   :style {:margin-bottom 25}}

           [:> Col {:span 18}
            [filters]]

           [:> Col {:span 6}
            [:div {:style {:position "relative"}}
             [:> Button {:type "primary"
                         :className "archive-toggle-button"
                         :style {:height 50
                                 :border-radius 15
                                 :width "100%"
                                 :backgroundColor "#D3EAFF"
                                 :color "black"
                                 :overflow "hidden"
                                 :white-space "nowrap"
                                 :font-size 24
                                 :font-weight 300
                                 :box-shadow "0 2px 8px rgba(0, 4, 6, 0.25)"}
                         :onClick (fn []
                                    (swap! app-state assoc :selected_units_catalog [])
                                    (swap! app-state assoc :products_mode (case @products_mode
                                                                             "catalog" "archive"
                                                                             "archive" "catalog")))}

              (case @products_mode
                "catalog" "Перейти в архив"
                "archive" "Перейти в каталог")]
             [:> Button {:shape "circle"
                         :style {:position "absolute"
                                 :top "50%"
                                 :right 10
                                 :transform "translateY(-50%)"
                                 :zIndex 2}
                         :icon (as-element [:> icons/QuestionCircleOutlined])
                         :onClick (fn []
                                    (swap! app-state assoc :units_help_type "archive")
                                    (swap! app-state assoc :units_help_modal_open? true))}]]]]


          [:div {:className "search-component"
                 :style {:position "relative"}}
           [units_list]
           [:> Button {:shape "circle"
                       :style {:position "absolute"
                               :top 12
                               :right 12
                               :zIndex 2}
                       :icon (as-element [:> icons/QuestionCircleOutlined])
                       :onClick (fn []
                                  (swap! app-state assoc :units_help_type "table")
                                  (swap! app-state assoc :units_help_modal_open? true))}]
           ]

          [footer]]])))
