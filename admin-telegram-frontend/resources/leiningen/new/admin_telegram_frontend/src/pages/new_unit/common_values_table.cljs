(ns {{name}}.pages.new-unit.common-values-table
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
        Form antd/Form
        FormItem (.-Item Form)
        Select antd/Select
        SelectOption (.-Option Select)
        AutoComplete antd/AutoComplete
        Button antd/Button
        Input antd/Input
        Spin antd/Spin
        form-values (reagent/atom {})
        ]

    (fn []
      (let [common-attrs (:common_attributes @second-lvl-config)
            has-filled-field? (some (fn [[key value]]
                                      (cond
                                        (string? value) (not (empty? value))
                                        (vector? value) (not (empty? value))
                                        :else (some? value)))
                                    @form-values)]

        [:> Form
         {:layout "horizontal"
          :variant "filled"
          :size "large"
          :onFinish (fn [values]
                      (let [unit_id (str (random-uuid))
                            filters-data (assoc (js->clj values :keywordize-keys true) :unit_id unit_id)]
                        (swap! app-state assoc :new_unit_common_attributes filters-data)))}

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
                            :label (:label option)})}]

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
            :disabled (not has-filled-field?)}
           "Сохранить"]]]))))
