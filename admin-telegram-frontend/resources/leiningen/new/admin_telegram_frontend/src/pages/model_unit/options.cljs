(ns {{name}}.pages.model-unit.options
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [clojure.string :as string]
   [{{name}}.events.product-add :refer [product_add]]
   )
  )


(defn join [sep coll]
  (reduce (fn [acc s] (str acc (when (seq acc) sep) s)) "" coll))



(defn options []
  (let [
        Row antd/Row
        Col antd/Col
        Button antd/Button

        model_unit (reagent/cursor app-state [:model_unit])
        products_mode (reagent/cursor app-state [:products_mode])
        search_value (reagent/cursor app-state [:search_value])
        second-lvl-config (reagent/cursor app-state [:second_lvl_config])
        second-level-common-attributes (reagent/cursor app-state [:second_level_common_attributes])
        ]
    (fn []
      [:> Row {:gutter 16
               :style {:margin-bottom 25}}

       [:> Col {:span 2
                :style {:display "flex"
                        :flex-direction "column"
                        :height "100%"}}
        [:> Button {:style {:background "#D3EAFF"
                            :color "black"
                            :height 50
                            :border-radius 15
                            :width "100%"
                            :font-size 24
                            :font-weight 300
                            :box-shadow "0 2px 8px rgba(0, 2, 5, 0.25)"
                            :margin-top "auto"}
                    :onClick (fn []
                               (swap! app-state assoc :second_lvl_config {})
                               (swap! app-state assoc :second_level_common_attributes {})
                               (set! (.-href (.-location js/window)) (str "#/units"))
                               )
                    :icon (as-element [:> icons/ArrowLeftOutlined])}]]


       [:> Col {:span 16}
        [:div {:style {:display "flex" :gap 8 :align-items "center"}}
         [:> Button {:type "primary"
                     :style {:flex 1
                             :height 50
                             :border-radius 15
                             :overflow "hidden"
                             :white-space "nowrap"
                             :backgroundColor "#D3EAFF"
                             :box-shadow "0 2px 8px rgba(0, 4, 6, 0.25)"
                             :color "black"
                             :font-size 24
                             :font-weight 300}
                     :onClick (fn []
                                (let [config-attributes (mapv keyword (conj (mapv :attribute_name (:common_attributes @second-lvl-config)) "unit_id"))
                                      common-attributes (select-keys @second-level-common-attributes config-attributes)]
                                  (product_add common-attributes)
                                  )
                                )
                     }
          "Добавить модификацию"]
         [:> Button {:shape "circle"
                     :icon (as-element [:> icons/QuestionCircleOutlined])
                     :onClick (fn []
                                (swap! app-state assoc :model_unit_help_type "add_modification")
                                (swap! app-state assoc :model_unit_help_modal_open? true))}]]]


         [:> Col {:span 6}
          [:div {:style {:display "flex" :gap 8 :align-items "center"}}
           [:> Button {:type "primary"
                       :style {:flex 1
                               :height 50
                               :border-radius 15
                               :overflow "hidden"
                               :white-space "nowrap"
                               :box-shadow "0 2px 8px rgba(0, 4, 6, 0.25)"
                               :backgroundColor "#D3EAFF"
                               :color "black"
                               :font-size 24
                               :font-weight 300}

                       :onClick (fn []
                                  (swap! app-state assoc :selected_units_model_unit [])
                                  (swap! app-state assoc :products_mode (case @products_mode
                                                                          "catalog" "archive"
                                                                          "archive" "catalog")))}
            (case @products_mode
              "catalog" "Перейти в архив"
              "archive" "Перейти в каталог")]
           [:> Button {:shape "circle"
                       :icon (as-element [:> icons/QuestionCircleOutlined])
                       :onClick (fn []
                                  (swap! app-state assoc :model_unit_help_type "archive_toggle")
                                  (swap! app-state assoc :model_unit_help_modal_open? true))}]]]
         ]





      )
      )
      )
