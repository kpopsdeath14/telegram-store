(ns jemo-murge-admin-frontend.pages.units.modal-product-add
  (:require
   ["antd" :as antd]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   ["@ant-design/icons" :as icons]
   [reagent.core :as reagent :refer [as-element]])
  )

(defn modal_product_add []
  (let [Modal antd/Modal
        Input antd/Input
        AutoComplete antd/AutoComplete
        Select antd/Select
        Button antd/Button

        visible? (reagent/cursor app-state [:show_modal_product_add?])
        filters (reagent/cursor app-state [:filters])
        current_articul (reagent/cursor app-state [:current_articul])
        ]
    (fn []
      [:> Modal
       {:visible @visible?
        :closable true
        :width "80vw"
        :style {:top "35%"
                :maxWidth "800px"}
        :onCancel #(swap! app-state assoc :show_modal_product_add? false)
        
        :footer (fn [_ {:keys [OkBtn CancelBtn]}]
                  (as-element
                   (let [required-fields [:type :vendor_code :name]
                         disabled? (some (fn [field]
                                           (let [value (get @current_articul field)]
                                             (or (nil? value)
                                                 (and (string? value)
                                                      (= "" value)))))
                                         required-fields)
                         ]
                     [:> Button
                      {:style {:background "#D3EAFF"
                               :color "black"
                               :height 50
                               :border-radius 15
                               :width "auto"
                               :font-size 24
                               :font-weight 300
                               :box-shadow "0 2px 8px rgba(0, 4, 6, 0.25)"}
                       :disabled disabled?
                       :onClick (fn []
                                  (swap! app-state assoc :model_unit [{} {} {} {}])
                                  (swap! app-state assoc :adding_new_product? true)
                                  (swap! app-state assoc :page :model_unit)
                                  (swap! app-state assoc :current_vendor_code (:vendor_code (:current_articul @app-state)))
                                  (set! (.-href (.-location js/window)) (str "#/model-unit/" (js/encodeURIComponent (:vendor_code (:current_articul @app-state)))))
                                  (swap! app-state assoc :show_modal_product_add? false)
                                  )
                       }
                      "Сохранить"])
                   )
                  )
        }

       [:div {:style {:width "100%"
                      :padding 16
                      :boxSizing "border-box"
                      }
              } 
        
        [:> Input {:style {:width "100%"
                           :height 50
                           :border "1px solid #00247C"
                           :border-radius 6
                           :margin-bottom 12
                           }
                   :value (:vendor_code @current_articul)
                   :placeholder "Введите артикул товара"
                   :onChange (fn [event]
                               (let [value (.-value (.-target event))]
                                 (swap! app-state
                                        assoc-in
                                        [:current_articul :vendor_code]
                                        value)
                                 
                                 (swap! app-state
                                        assoc-in
                                        [:articul_changes :vendor_code]
                                        value) 
                                 )
                               )
                   }
         ]
        
        [:> Input {:style {:width "100%"
                           :height 50
                           :border "1px solid #00247C"
                           :border-radius 6
                           :margin-bottom 12}
                   :value (:name @current_articul)
                   :placeholder "Введите наименование товара"
                   :onChange (fn [event]
                               (let [value (.-value (.-target event))]
                                 (swap! app-state
                                        assoc-in
                                        [:current_articul :name]
                                        value)
                                 
                                 (swap! app-state
                                        assoc-in
                                        [:articul_changes :name]
                                        value) 
                                 ) 
                               )
                   }
         ]
        
        [:div {:style {:display "flex"
                       :gap 25
                       }
               }
         [:> AutoComplete {:style {:width "100%"
                             :height 50
                             :border "3px solid #D3EAFF"
                             :border-radius 15
                             :box-shadow "none"
                             }
                     :value (:type @current_articul)
                     :bordered false
                     :placeholder "Тип"
                     :options (let [type-data (first (filter #(= "type" (:attribute_name %)) @filters))]
                                (map (fn [v] {:label v, :value v})
                                     (:attribute_values type-data)))
                     :onChange (fn [values]
                                 (swap! app-state
                                        assoc-in
                                        [:current_articul :type]
                                        (js->clj values))
                                 
                                 (swap! app-state
                                        assoc-in
                                        [:articul_changes :type]
                                        (js->clj values)) 
                                 )
                     } 
          ]
         [:> Select {:style {:width "100%"
                             :height "auto"
                             :min-height 50
                             :border "3px solid #D3EAFF"
                             :border-radius 15
                             :box-shadow "none"}
                     :value (:categories @current_articul)
                     :bordered false
                     :placeholder "Категории"
                     :mode "multiple"
                     :options (let [type-data (first (filter #(= "categories" (:attribute_name %)) @filters))]
                                (map (fn [v] {:label v, :value v})
                                     (:attribute_values type-data)))
                     :onChange (fn [values]
                                 (swap! app-state
                                        assoc-in
                                        [:current_articul :categories]
                                        (js->clj values))
                                 
                                 (swap! app-state
                                        assoc-in
                                        [:articul_changes :categories]
                                        (js->clj values))
                                 )
                     }
          ]
         [:> AutoComplete {:style {:width "100%"
                             :height 50
                             :border "3px solid #D3EAFF"
                             :border-radius 15
                             :box-shadow "none"}
                     :value (:collection @current_articul)
                     :bordered false
                     :placeholder "Коллекция"

                     :options (let [type-data (first (filter #(= "collection" (:attribute_name %)) @filters))]
                                (map (fn [v] {:label v, :value v})
                                     (:attribute_values type-data)))
                     :onChange (fn [values]
                                 (swap! app-state
                                        assoc-in
                                        [:current_articul :collection]
                                        (js->clj values))
                                 
                                 (swap! app-state
                                        assoc-in
                                        [:articul_changes :collection]
                                        (js->clj values)) 
                                 )
                     }
          ]
         ]
        
        
        ]
       ]
      )
    )
    )
