(ns jemo-murge-admin-frontend.pages.product.price
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [jemo-murge-admin-frontend.events.price-set :refer [price_set]])
  )


(defn price []
  (let [Form antd/Form
        FormItem (.-Item Form)
        Input antd/Input
        Button antd/Button
        Space antd/Space
        Spin antd/Spin
        
        product (reagent/cursor app-state [:product])
        product-server (reagent/cursor app-state [:product_server])
        form-values (reagent/atom {})]

    (fn []
      (let [initial-values {:telegram_price (get-in @product [:prices :telegram_price :price])
                            :discount_price (get-in @product [:prices :discount_price :price])}
            server-values {:telegram_price (get-in @product-server [:prices :telegram_price :price])
                           :discount_price (get-in @product-server [:prices :discount_price :price])}
            draft-prices (get-in @app-state [:product_draft :prices])
            draft-values (cond-> {}
                           (some? (get-in draft-prices [:telegram_price :price]))
                           (assoc :telegram_price (get-in draft-prices [:telegram_price :price]))
                           (some? (get-in draft-prices [:discount_price :price]))
                           (assoc :discount_price (get-in draft-prices [:discount_price :price])))
      
            has-changes? (or
                          (some (fn [[key value]]
                                  (not= (str value) (str (get server-values key))))
                                @form-values)
                          (some (fn [[key value]]
                                  (not= (str value) (str (get server-values key))))
                                draft-values))]
      
        [:> Space {:direction "vertical" :style {:background-color "#F2F2F2"
                                                 :width "100%"
                                                 :border-radius 10
                                                 :padding 20}}
         [:h1 "Цены"]

         (if (seq @product)
           [:> Form {:initialValues initial-values
                     :onFinish (fn [values]
                                 (swap! app-state assoc :scroll_restore_y (.-scrollY js/window))
                                 (let [filters-data (mapv (fn [key]
                                                            (let [attribute_name (name key)
                                                                  attribute_value (key (js->clj values :keywordize-keys true))]
                                                              {:product_id (:current_product_id @app-state)
                                                               :price_type_name (name attribute_name)
                                                               :price attribute_value}))
                                                          (keys (js->clj values :keywordize-keys true)))]
                                   (price_set filters-data)
                                   (swap! app-state update :product_draft
                                          (fn [draft]
                                            (let [updated (update (or draft {}) :prices
                                                                  (fn [prices]
                                                                    (let [cleaned (dissoc (or prices {}) :telegram_price :discount_price)]
                                                                      (when (seq cleaned) cleaned))))]
                                              (if (and (map? updated) (empty? (:prices updated)))
                                                (dissoc updated :prices)
                                                updated))))
           
                                   (reset! form-values {})))}
            [:> FormItem {:name "telegram_price"
                          :label "Цена"}
             [:> Input
              {:placeholder "Введите цену"
               :onChange (fn [event]
                           (let [value (.. event -target -value)]
                             (swap! form-values assoc :telegram_price value)
                             (swap! app-state assoc-in [:product_draft :prices :telegram_price :price] value)))}]]
           
            [:> FormItem {:name "discount_price"
                          :label "Цена со скидкой"}
             [:> Input
              {:placeholder "Введите цену со скидкой"
               :onChange (fn [event]
                           (let [value (.. event -target -value)]
                             (swap! form-values assoc :discount_price value)
                             (swap! app-state assoc-in [:product_draft :prices :discount_price :price] value)))}]]
           
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
