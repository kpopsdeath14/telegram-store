(ns jemo-murge-admin-frontend.pages.product.common-parameters
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   )
  )


(defn common_parameters []
  (let [filters (reagent/cursor app-state [:filters])
        third-lvl-config (reagent/cursor app-state [:third_lvl_config])
        product (reagent/cursor app-state [:product])
        Form antd/Form
        FormItem (.-Item Form)
        Select antd/Select
        SelectOption (.-Option Select)
        Button antd/Button
        Input antd/Input
        Spin antd/Spin
        Space antd/Space

        ]

    (fn []
      (let [common_attributes (:common_attributes @third-lvl-config)
            common-values @product
            ]
        [:> Space {:direction "vertical" :style {:background-color "#F2F2F2"
                                                 :width "100%"
                                                 :border-radius 10
                                                 :padding 20
                                                 :margin-bottom 20}}
         [:div {:style {:display "flex" :align-items "center" :gap 8}}
          [:h1 "Общие атрибуты"]
          [:> Button {:shape "circle"
                      :icon (as-element [:> icons/QuestionCircleOutlined])
                      :onClick (fn []
                                 (swap! app-state assoc :product_help_type "common_attrs")
                                 (swap! app-state assoc :product_help_modal_open? true))}]]

         (if (and (seq common_attributes) (seq common-values)) 
           
           [:> Form
            {:layout "horizontal"
             :initialValues common-values
             :variant "filled"
             :size "large"
             :disabled true}
           
           
            (for [{:keys [ord attribute_name form_field_type attribute_name_rus placeholder] :as field}
                  common_attributes]
              ^{:key attribute_name}
              (if-not (= (name attribute_name) "images")
                [:> FormItem
                 {:name (name attribute_name)
                  :label attribute_name_rus}
           
                 (case form_field_type
                   "SELECT"
                   [:> Select
                    {:placeholder (or placeholder (str "Выберите " attribute_name_rus))
                     :allowClear true
                     :disabled true}
                    (for [option (:attribute_values (first (filter #(= (:attribute_name %) (name attribute_name)) @filters)))]
                      [:> SelectOption
                       {:value option}
                       (:label option)])]
           
                   "MULTISELECT"
                   [:> Select
                    {:mode "tags"
                     :placeholder (or placeholder (str "Выберите " attribute_name_rus))
                     :allowClear true
                     :disabled true}
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
                     :disabled true}]
           
                   ;; fallback
                   [:> Input
                    {:value (str "Unknown field type: " form_field_type)
                     :disabled true}])]))]
         
           [:> Spin {:size "large"} "Загрузка..."])
        
         ]
        )
      )
    )
    )
