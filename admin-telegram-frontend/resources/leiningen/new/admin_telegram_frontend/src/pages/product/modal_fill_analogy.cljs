(ns {{name}}.pages.product.modal-fill-analogy
  (:require
   ["antd" :as antd]
   [{{name}}.db :refer [app-state]]
   ["@ant-design/icons" :as icons]
   [reagent.core :as reagent :refer [as-element]]
   [{{name}}.events.product-get-to-fill :refer [product_get_to_fill]]
   ))

(defn modal_fill_analogy []
  (let [Form antd/Form
        FormItem (.-Item Form)
        Modal antd/Modal
        Input antd/Input
        Select antd/Select
        SelectOption (.-Option Select)
        Button antd/Button

        visible? (reagent/cursor app-state [:show_modal_fill_analogy?])
        third-lvl-config (reagent/cursor app-state [:third_lvl_config])
        ]
    (fn []
      [:> Modal
       {:visible @visible?
        :closable true
        :footer nil
        :width "80vw"
        :style {:top "35%"
                :maxWidth "800px"}
        :onCancel #(swap! app-state assoc :show_modal_fill_analogy? false)
        }

       [:div {:style {:width "100%"
                      :padding 16
                      :boxSizing "border-box"}}
        

        [:> Form
         {:layout "horizontal"
          :variant "filled"
          :size "large"
          :onFinish (fn [values]
                      (swap! app-state assoc :product_to_fill {:prev_data (:product @app-state)
                                                               :parameters_to_fill (mapv #(keyword %) (:parameters_to_fill (js->clj values :keywordize-keys true)))
                                                               }
                             ) 
                      (swap! app-state assoc :product {})
                      (product_get_to_fill (assoc {} :product_id [(:product_id (js->clj values :keywordize-keys true))]))
                      (swap! app-state assoc :show_modal_fill_analogy? false)
                      
                      )
          
          :footer (fn []
                    (as-element 
                     [:div]
                     )
                    )
          }
         
         [:> FormItem {:name "product_id"
                       :label "ID товара"
                       }
          [:> Input {:placeholder "Введите product_id товара"}]
          ]
         
         [:> FormItem {:name "parameters_to_fill"
                       :label "Поля для заполнения"
                       }
          [:> Select
           {:mode "tags"
            :placeholder "Выберите поля для заполнения"
            :allowClear true
            :style {:width "100%"}}
           (doall
           (for [option (or (->> (:modification_attributes @third-lvl-config)
                                 (remove #(= "product_id" (:attribute_name %)))
                                 (mapv (fn [attribute]
                                         {:value (:attribute_name attribute)
                                          :label (:attribute_name_rus attribute)})))
                            [])]
              ^{:key (:value option)}
              [:> SelectOption
               {:value (:value option)}
               (:label option)]))]
          ] 
         
         [:> FormItem
          [:> Button
           {:type "primary"
            :htmlType "submit"}
           "Сохранить"]]
         
         ]

        
        ]
       ]
        )
        )
        )
