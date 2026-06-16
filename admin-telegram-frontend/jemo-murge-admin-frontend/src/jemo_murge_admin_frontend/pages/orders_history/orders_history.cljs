(ns jemo-murge-admin-frontend.pages.orders-history.orders-history
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [clojure.string :as string]
   [jemo-murge-admin-frontend.pages.orders-history.filters :refer [filters]]
   [jemo-murge-admin-frontend.pages.orders-history.modal-help :refer [modal_help]]
   )
  )

(defn status->color [status-name]
  (let [normalized (some-> status-name
                           string/trim
                           string/lower-case
                           (string/replace #"ё" "е"))]
    (case normalized
      "заказ оформлен" "#1890ff"
      "заказ оплачен" "#52c41a"
      "в доставке" "#faad14"
      "доставлен" "#13c2c2"
      "завершен" "#389e0d"
      "#8c8c8c")))




(defn orders_history []
  (let [
        List antd/List
        Button antd/Button
        orders_history (reagent/cursor app-state [:orders_history])
        ]
    (fn []
      [:div
       [modal_help]
       [:div {:style {:display "flex"
                      :align-items "center"
                      :gap 16
                      :margin-bottom 24}}
        [:> Button {:style {:background "#D3EAFF"
                            :color "black"
                            :height 44
                            :border-radius 14
                            :font-size 16
                            :font-weight 400
                            :box-shadow "0 2px 8px rgba(0, 2, 5, 0.15)"}
                    :icon (as-element [:> icons/ArrowLeftOutlined])
                    :onClick (fn []
                               (set! (.-href (.-location js/window)) "#/units"))}
         "Назад"]
        [:div
         [:div {:style {:display "flex" :align-items "center" :gap 8}}
          [:div {:style {:font-size 24
                         :font-weight 700}}
           "Заказы"]
          [:> Button {:shape "circle"
                      :icon (as-element [:> icons/QuestionCircleOutlined])
                      :onClick (fn []
                                 (swap! app-state assoc :orders_history_help_type "page")
                                 (swap! app-state assoc :orders_history_help_modal_open? true))}]]
         [:div {:style {:font-size 13
                        :color "#8c8c8c"
                        :margin-top 4}}
          "История заказов и фильтры"]
         ]
        ]
       [filters]

       [:> List {:style {:margin-top 25
                         :margin-bottom 25} 
                 :locale {:emptyText "заказов нет"}
                 :dataSource @orders_history 
                 :render-item (fn [order]
                                (let [order (js->clj order :keywordize-keys true)
                                      item-count (-> order :jsonb_agg count)
                                      total-sum (reduce + (map :final_summ (:jsonb_agg order)))
                                      status-name (-> order :order_current_status :status_name_rus)
                                      status-color (status->color status-name)]
                                  (as-element
                                   [:div {:style {:background-color "white"
                                                  :box-sizing "border-box"
                                                  :padding "24px"
                                                  :display "flex"
                                                  :flex-direction "column"
                                                  :gap 16
                                                  :border-radius 12
                                                  :border "1px solid #f0f0f0"
                                                  :margin-bottom 16
                                                  :transition "all 0.3s ease"
                                                  :cursor "pointer"
                                                  :box-shadow "0 2px 8px rgba(0, 0, 0, 0.06)"
                                                  :position "relative"}
                                          :on-click (fn [e]
                                                      (js/window.scrollTo 0 0)
                                                      (set! (.-href (.-location js/window))
                                                            (str "#/order/" (js/encodeURIComponent (:order_id order)))))}
                                    ;; Верхняя строка - основная информация
                                    [:div {:style {:display "flex"
                                                   :justify-content "space-between"
                                                   :align-items "flex-start"
                                                   :gap 16
                                                   :flex-wrap "wrap"}}
                                     ;; Левая часть - номер заказа
                                     [:div {:style {:display "flex"
                                                    :flex-direction "column"
                                                    :gap 4}}
                                      [:span {:style {:color "#8c8c8c"
                                                      :font-size "12px"
                                                      :font-weight 400}}
                                       "Номер заказа"]
                                      [:span {:style {:color "#262626"
                                                      :font-size "18px"
                                                      :font-weight 500
                                                      :word-break "break-word"}}
                                       (:order_external_id order)]]
                                     ;; Центральная часть - статус
                                     [:div {:style {:display "flex"
                                                    :flex-direction "column"
                                                    :align-items "center"
                                                    :gap 4}}
                                      [:span {:style {:color "#8c8c8c"
                                                      :font-size "12px"
                                                      :font-weight 400}}
                                       "Статус"]
                                      [:span {:style {:color status-color
                                                      :font-size "14px"
                                                      :font-weight 500
                                                      :padding "4px 12px"
                                                      :background-color (str status-color "10")
                                                      :border-radius "12px"}}
                                       status-name]]
                                     ;; Правая часть - пользователь
                                     [:div {:style {:display "flex"
                                                    :flex-direction "column"
                                                    :align-items "flex-end"
                                                    :gap 4}}
                                      [:span {:style {:color "#8c8c8c"
                                                      :font-size "12px"
                                                      :font-weight 400}}
                                       "Пользователь"]
                                      [:span {:style {:color "#262626"
                                                      :font-size "14px"
                                                      :font-weight 400}}
                                       (:telegram_user_id order)]]]
                                    ;; Нижняя строка - детали заказа
                                    [:div {:style {:display "flex"
                                                   :justify-content "space-between"
                                                   :align-items "center"
                                                   :padding-top 16
                                                   :border-top "1px solid #f0f0f0"
                                                   :gap 16
                                                   :flex-wrap "wrap"}}
                                     [:div {:style {:display "flex"
                                                    :align-items "center"
                                                    :gap 8}}
                                      [:span {:style {:color "#52c41a"
                                                      :font-size "16px"}}
                                       "✓"]
                                      [:span {:style {:color "#262626"
                                                      :font-size "14px"}}
                                       (str item-count " "
                                            (case item-count
                                              1 "товар"
                                              (if (and (>= item-count 5) (<= item-count 20))
                                                "товаров"
                                                "товара")))]]
                                     (str total-sum " ₽")]
                                    [:div {:style {:position "absolute"
                                                   :top 0
                                                   :left 0
                                                   :width 4
                                                   :height "100%"
                                                   :background-color status-color
                                                   :border-radius "2px 0 0 2px"}
                                           }
                                     ]
                                    ]
                                    )
                                    )
                                    )
                                    }
       ]
       ]
      )
    )
  )
