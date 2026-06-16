(ns {{name}}.pages.model-unit.common-values-table
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [{{name}}.events.product-attribute-add :refer [product_filter_attribute_add]]
   )
  )


(defn common_values_table []
  (let [filters (reagent/cursor app-state [:filters])
        second-lvl-config (reagent/cursor app-state [:second_lvl_config])
        second-level-common-attributes (reagent/cursor app-state [:second_level_common_attributes])
        Form antd/Form
        FormItem (.-Item Form)
        Select antd/Select
        SelectOption (.-Option Select)
        Button antd/Button
        Input antd/Input
        Spin antd/Spin
        Space antd/Space
        AutoComplete antd/AutoComplete

        form-values (reagent/atom {})
        ]

    (fn []
      (let [common-attrs (:common_attributes @second-lvl-config)
            common-values @second-level-common-attributes
            has-changes? (some (fn [[key value]]
                                 (not= (str value) (str (get common-values key))))
                               @form-values)
            ]
        
        [:> Space {:direction "vertical" :style {:background-color "#F2F2F2"
                                                 :width "100%"
                                                 :border-radius 10
                                                 :padding 20}}
         [:div {:style {:display "flex" :align-items "center" :gap 8}}
          [:h1 "Общие атрибуты"]
          [:> Button {:shape "circle"
                      :icon (as-element [:> icons/QuestionCircleOutlined])
                      :onClick (fn []
                                 (swap! app-state assoc :model_unit_help_type "common_attrs")
                                 (swap! app-state assoc :model_unit_help_modal_open? true))}]]

         (if (and (seq common-attrs) (seq common-values))
         
           [:> Form
            {:layout "horizontal"
             :initialValues common-values
             :variant "filled"
             :size "large"
             :onFinish (fn [values]
                         (let [filters-data (mapv (fn [key]
                                                    (let [attribute_name (name key)
                                                          attribute_value (key (js->clj values :keywordize-keys true))]
                                                      {:filters [{:attribute_name "unit_id"
                                                                  :attribute_values [(:current_unit_id @app-state)]}]
                                                       :set_attribute_name (name attribute_name)
                                                       :set_attribute_value attribute_value}))
                                                  (keys (js->clj values :keywordize-keys true)))]
                           (reset! form-values {})
                           (swap! app-state assoc :second_level_common_attributes {})
                           (product_filter_attribute_add filters-data)))}
           
            (for [{:keys [ord attribute_name form_field_type attribute_name_rus placeholder] :as field}
                  common-attrs]
              ^{:key attribute_name}
              [:> FormItem
               {:name (name attribute_name)
                :label attribute_name_rus}
           
               (case form_field_type
                 "SELECT"
                 [:> AutoComplete
                  {:placeholder (or placeholder (str "Выберите " attribute_name_rus))
                   :allowClear true
                   :onChange #(swap! form-values assoc (keyword attribute_name) %)
                   :options (for [option (:attribute_values (first (filter #(= (:attribute_name %) (name attribute_name)) @filters)))]
                              {:value option
                               :label (:label option)})
                   }
                  ]
           
                 "MULTISELECT"
                 [:> Select
                  {:mode "tags"
                   :placeholder (or placeholder (str "Выберите " attribute_name_rus))
                   :allowClear true
                   :onChange #(swap! form-values assoc (keyword attribute_name) %)}
                  (doall
                   (for [option (or (:attribute_values
                                     (first (filter #(= (:attribute_name %) (name attribute_name)) @filters)))
                                    [])]
                     ^{:key option}
                     [:> SelectOption
                      {:value option}
                      option]))]
           
                 "INPUT"
                 [:> Input
                  {:placeholder (or placeholder (str "Введите " attribute_name_rus))
                   :onChange #(swap! form-values assoc (keyword attribute_name) (.. % -target -value))}]
           
                 ;; fallback
                 [:div "Unknown field type: " (str form_field_type)])])
           
            [:> FormItem
             [:> Button
              {:type "primary"
               :htmlType "submit"
               :disabled (not has-changes?)}
              "Сохранить"]]]
         
           [:> Spin {:size "large"} "Загрузка..."]
           
           )
         ]
        )
        )
        )
        )
