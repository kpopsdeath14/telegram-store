(ns jemo-murge-admin-frontend.pages.product.options
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [jemo-murge-admin-frontend.pages.product.modal-fill-analogy :refer [modal_fill_analogy]]
   [jemo-murge-admin-frontend.events.product-attribute-add :refer [product_filter_attribute_add]]
   ))




(defn options []
  (let [Row antd/Row
        Col antd/Col
        Input antd/Input
        Button antd/Button

        product_editing? (reagent/cursor app-state [:product_editing?])
        product_changes (reagent/cursor app-state [:product_changes]) 
        ]
    (fn []
      [:> Row {:gutter 16
               :style {:margin-bottom 25}
               }
       [:> Col {:span 2
                :style {:display "flex"
                        :height "100%"
                        }
                }
      
        [:> Button {:style {:background "#D3EAFF"
                            :color "black"
                            :height 50
                            :border-radius 15
                            :box-shadow "0 2px 8px rgba(0, 2, 5, 0.25)"
                            :width "100%"
                            :font-size 24
                            :font-weight 300}
                    :onClick (fn []
                               (swap! app-state assoc :page :model_unit) 
                               (set! (.-href (.-location js/window)) (str "#/model-unit/" (js/encodeURIComponent (:unit_id (:product @app-state)))))
                               (swap! app-state assoc :third_lvl_config {})
                               (swap! app-state assoc :product {})
                               )
                    :icon (as-element [:> icons/ArrowLeftOutlined])}]]
      
       [:> Col {:span 12}
        [:div {:style {:display "flex" :align-items "center" :gap 8}}
         [:> Button {:type "primary"
                     :style {:height 50
                             :border-radius 15
                             :overflow "hidden"
                             :flex 1
                             :white-space "nowrap"
                             :backgroundColor "#D3EAFF"
                             :box-shadow "0 2px 8px rgba(0, 4, 6, 0.25)"
                             :color "black"
                             :font-size 24
                             :font-weight 300}
                     :onClick (fn []
                                (let [filters-data {:filters [{:attribute_name "product_id"
                                                               :attribute_values [(:product_id (:product @app-state))]}]
                                                    :set_attribute_name "actual"
                                                    :set_attribute_value (case (:actual (:product @app-state))
                                                                           "true" "false"
                                                                           "false" "true"
                                                                           nil
                                                                           )
                                                    }
                                      ]
                                  (product_filter_attribute_add filters-data)
                                  )
                                )
                     }
          (str "Перевести в "
               (case (:actual (:product @app-state))
                 "true" "архив"
                 "false" "каталог"
                 nil
                 )
               )
          ]
         [:> Button {:shape "circle"
                     :icon (as-element [:> icons/QuestionCircleOutlined])
                     :onClick (fn []
                                (swap! app-state assoc :product_help_type "archive_toggle")
                                (swap! app-state assoc :product_help_modal_open? true))}]
         ]
        ]
       
       [:> Col {:span 10}
        [modal_fill_analogy]
        [:div {:style {:display "flex" :align-items "center" :gap 8}}
         [:> Button {:type "primary"
                     :style {:height 50
                             :border-radius 15
                             :overflow "hidden"
                             :flex 1
                             :white-space "nowrap"
                             :backgroundColor "#D3EAFF"
                             :box-shadow "0 2px 8px rgba(0, 4, 6, 0.25)"
                             :color "black"
                             :font-size 24
                             :font-weight 300}
                     :onClick (fn []
                                (swap! app-state assoc :show_modal_fill_analogy? true)
                                )
                     }
          "Заполнить по аналогии"
          ]
         [:> Button {:shape "circle"
                     :icon (as-element [:> icons/QuestionCircleOutlined])
                     :onClick (fn []
                                (swap! app-state assoc :product_help_type "fill_analogy")
                                (swap! app-state assoc :product_help_modal_open? true))}]
         ]
         ]
       ] 
      )
    )
  )
