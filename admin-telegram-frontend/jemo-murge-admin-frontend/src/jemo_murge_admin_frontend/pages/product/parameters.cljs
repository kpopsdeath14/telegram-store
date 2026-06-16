(ns jemo-murge-admin-frontend.pages.product.parameters
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [jemo-murge-admin-frontend.events.product-attribute-add :refer [product_filter_attribute_add]]
   )
  )

(defn- description-field? [attribute-name attribute-name-rus]
  (let [attribute-name (name attribute-name)
        attribute-name-rus (or attribute-name-rus "")]
    (or (= attribute-name-rus "описание товара")
        (= attribute-name-rus "уход за изделием")
        )
    )
  )


(defn parameters []
  (let [filters (reagent/cursor app-state [:filters])
        third-lvl-config (reagent/cursor app-state [:third_lvl_config])
        product (reagent/cursor app-state [:product])
        product-server (reagent/cursor app-state [:product_server])
        Form antd/Form
        FormItem (.-Item Form)
        Select antd/Select
        SelectOption (.-Option Select)
        AutoComplete antd/AutoComplete
        Button antd/Button
        Input antd/Input
        TextArea (.-TextArea Input)
        Spin antd/Spin
        Space antd/Space
        form-values (reagent/atom {})
        ]

    (fn []
      (let [modification_attributes (:modification_attributes @third-lvl-config)
            common-values @product
            server-values (or @product-server {})
            special-attribute-names #{"color" "size"}
            special-fields (filter (fn [{:keys [attribute_name]}]
                                     (special-attribute-names (name attribute_name)))
                                   modification_attributes)
            regular-fields (remove (fn [{:keys [attribute_name]}]
                                     (special-attribute-names (name attribute_name)))
                                   modification_attributes)
            draft-values (dissoc (or (:product_draft @app-state) {}) :prices)
            has-changes? (or
                          (some (fn [[key value]]
                                  (not= (str value) (str (get server-values key))))
                                @form-values)
                          (some (fn [[key value]]
                                  (not= (str value) (str (get server-values key))))
                                draft-values))
            ] 
        
        [:> Space {:direction "vertical" :style {:background-color "#F2F2F2"
                                                 :width "100%"
                                                 :border-radius 10
                                                 :padding 20
                                                 :margin-bottom 20}}
         [:div {:style {:display "flex" :align-items "center" :gap 8}}
          [:h1 "Атрибуты конкретно этой модификации"]
          [:> Button {:shape "circle"
                      :icon (as-element [:> icons/QuestionCircleOutlined])
                      :onClick (fn []
                                 (swap! app-state assoc :product_help_type "specific_attrs")
                                 (swap! app-state assoc :product_help_modal_open? true))}]]

         (if (and (seq modification_attributes) (seq common-values))
           
           [:> Form
            {:layout "horizontal"
             :initialValues common-values
             :variant "filled"
             :size "large"
             :onFinish (fn [values]
                         (swap! app-state assoc :scroll_restore_y (.-scrollY js/window))
                         (let [attribute-values (js->clj values :keywordize-keys true)
                               product-id (or (:current_product_id @app-state)
                                              (:product_id @product)) 
                               filters-data (mapv (fn [key]
                                                    (let [attribute_name (name key)
                                                          attribute_value (key attribute-values)]
                                                      {:filters [{:attribute_name "product_id"
                                                                  :attribute_values [(:current_product_id @app-state)]}]
                                                       :set_attribute_name (name attribute_name)
                                                       :set_attribute_value attribute_value}))
                                                  (keys attribute-values))
                               ]
                           (product_filter_attribute_add filters-data) 
                           (swap! app-state update :product_draft
                                  (fn [draft]
                                    (let [draft (or draft {})
                                          cleaned (apply dissoc draft (keys attribute-values))]
                                      cleaned)))
                           (reset! form-values {}))
                         )
             }
           
            (when (seq special-fields)
              [:div {:style {:border "1px solid #ef4444"
                             :border-radius 12
                             :padding 16
                             :background "#fff5f5"
                             :margin-bottom 16}}
               [:div {:style {:font-size 13
                              :font-weight 600
                              :color "#b91c1c"
                              :margin-bottom 12}}
                "Цвет и размер обязательны и должны быть уникальны в этой модели."]

               (for [{:keys [ord attribute_name form_field_type attribute_name_rus placeholder] :as field}
                     special-fields]
                 ^{:key attribute_name}
                 (let [product_id_field? (= (name attribute_name) "product_id")
                       required-message (str "Заполните " (or attribute_name_rus (name attribute_name)))]
                   (if-not (= (name attribute_name) "images")
                     [:> FormItem
                      {:name (name attribute_name)
                       :label attribute_name_rus
                       :rules (clj->js [{:required true :message required-message}])}

                      (case form_field_type
                        "SELECT"
                         [:> AutoComplete
                          {:placeholder (or placeholder (str "Выберите " attribute_name_rus))
                           :allowClear true
                           :onChange (fn [value]
                                       (swap! form-values assoc (keyword attribute_name) value)
                                       (swap! app-state assoc-in [:product_draft (keyword (name attribute_name))] value))
                           :options (for [option (:attribute_values (first (filter #(= (:attribute_name %) (name attribute_name)) @filters)))]
                                      {:value option
                                       :label (:label option)})}]

                        "MULTISELECT"
                        [:> Select
                         {:mode "tags"
                          :placeholder (or placeholder (str "Выберите " attribute_name_rus))
                          :allowClear true
                          :onChange (fn [value]
                                      (swap! form-values assoc (keyword attribute_name) value)
                                      (swap! app-state assoc-in [:product_draft (keyword (name attribute_name))] value))}
                         (doall
                          (for [option (or (:attribute_values
                                            (first (filter #(= (:attribute_name %) (name attribute_name)) @filters)))
                                           [])]
                            ^{:key option}
                            [:> SelectOption
                             {:value option}
                             option]))]

                        "INPUT"
                        (if (description-field? attribute_name attribute_name_rus)
                          [:> TextArea
                           {:placeholder (or placeholder (str "Введите " attribute_name_rus))
                            :autoSize {:minRows 3}
                            :disabled product_id_field?
                            :onChange (fn [event]
                                        (let [value (.. event -target -value)]
                                          (swap! form-values assoc (keyword attribute_name) value)
                                          (swap! app-state assoc-in [:product_draft (keyword (name attribute_name))] value)))}]
                          [:> Input
                           {:placeholder (or placeholder (str "Введите " attribute_name_rus))
                            :disabled product_id_field?
                            :onChange (fn [event]
                                        (let [value (.. event -target -value)]
                                          (swap! form-values assoc (keyword attribute_name) value)
                                          (swap! app-state assoc-in [:product_draft (keyword (name attribute_name))] value)))}])

                        ;; fallback
                        [:div "Unknown field type: " (str form_field_type)])])))
                        ]
                        )

            (for [{:keys [ord attribute_name form_field_type attribute_name_rus placeholder] :as field}
                  regular-fields]
              ^{:key attribute_name}
              (let [product_id_field? (= (name attribute_name) "product_id")]
                (if-not (= (name attribute_name) "images")
                  [:> FormItem
                   {:name (name attribute_name)
                    :label attribute_name_rus}

                   (case form_field_type
                     "SELECT"
                     [:> AutoComplete
                      {:placeholder (or placeholder (str "Выберите " attribute_name_rus))
                       :allowClear true
                       :onChange (fn [value]
                                   (swap! form-values assoc (keyword attribute_name) value)
                                   (swap! app-state assoc-in [:product_draft (keyword (name attribute_name))] value))
                       :options (for [option (:attribute_values (first (filter #(= (:attribute_name %) (name attribute_name)) @filters)))]
                                  {:value option
                                   :label (:label option)})}]

                     "MULTISELECT"
                     [:> Select
                      {:mode "tags"
                       :placeholder (or placeholder (str "Выберите " attribute_name_rus))
                       :allowClear true
                       :onChange (fn [value]
                                   (swap! form-values assoc (keyword attribute_name) value)
                                   (swap! app-state assoc-in [:product_draft (keyword (name attribute_name))] value))}
                      (doall
                       (for [option (or (:attribute_values
                                         (first (filter #(= (:attribute_name %) (name attribute_name)) @filters)))
                                        [])]
                         ^{:key option}
                         [:> SelectOption
                          {:value option}
                          option]))]

                     "INPUT"
                     (if (description-field? attribute_name attribute_name_rus)
                       [:> TextArea
                        {:placeholder (or placeholder (str "Введите " attribute_name_rus))
                         :autoSize {:minRows 3}
                         :disabled product_id_field?
                         :onChange (fn [event]
                                     (let [value (.. event -target -value)]
                                       (swap! form-values assoc (keyword attribute_name) value)
                                       (swap! app-state assoc-in [:product_draft (keyword (name attribute_name))] value)))}]
                       [:> Input
                        {:placeholder (or placeholder (str "Введите " attribute_name_rus))
                         :disabled product_id_field?
                         :onChange (fn [event]
                                     (let [value (.. event -target -value)]
                                       (swap! form-values assoc (keyword attribute_name) value)
                                       (swap! app-state assoc-in [:product_draft (keyword (name attribute_name))] value)))}])

                     ;; fallback
                     [:div "Unknown field type: " (str form_field_type)])])))
            
            [:> FormItem
             {:name "quantity"
              :label "количество в наличии"}
             [:> Input
              {:placeholder (or placeholder (str "Введите количество"))
               :onChange (fn [event]
                           (let [value (.. event -target -value)]
                             (swap! form-values assoc :quantity value)
                             (swap! app-state assoc-in [:product_draft :quantity] value)))}]
             ]
           
            [:> FormItem
             [:> Button
              {:type "primary"
               :htmlType "submit"
               :disabled (not has-changes?)}
              "Сохранить"]
             ]
            ]
         
           [:> Spin {:size "large"} "Загрузка..."]
           )
         ]

        )
        )
        )
        )
