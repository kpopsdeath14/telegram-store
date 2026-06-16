(ns {{name}}.pages.orders-history.filters
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   )
  )





(defn filters []
  (let [Form antd/Form
        FormItem (.-Item Form)
        Select antd/Select
        Input antd/Input

        statuses (reagent/cursor app-state [:statuses])
        ]
    (fn []
      [:> Form
       {:layout "horizontal"
        :initialValues common-values
        :size "large"
        :onValuesChange (fn [values]
                          (swap! app-state assoc :orders_history_filters_picked
                                 (->> (js->clj values :keywordize-keys true)
                                      (map (fn [[k v]] [k (if (vector? v) v [v])]))
                                      (into {})
                                      (reduce-kv (fn [m k v]
                                                   (if (or (nil? v)
                                                           (and (string? v) (= "" v))
                                                           (and (vector? v) (empty? v))
                                                           (and (vector? v) (every? #(or (nil? %) (= "" %)) v)))
                                                     m
                                                     (assoc m k v)))
                                                 {}))))
        }
       [:> FormItem 
        {:name "status_names_filter"
         :label "Статус"}
        [:> Select
         {:mode "tags"
          :placeholder (or placeholder (str "Выберите статус"))
          :allowClear true
          :options (->> @statuses
                        (sort-by :status_order_number)
                        (map (fn [item]
                               {:label (:status_name_rus item)
                                :value (:status_name item)})))
          }
         ]
        ]
       
       [:> FormItem
        {:name "user_id_filter"
         :label "ID клиента"}
        [:> Input
         {:placeholder (or placeholder (str "Выберите ID клиента"))
          :allowClear true
          }
         ]
        ]
       
       [:> FormItem
        {:name "external_order_id_filter"
         :label "ID заказа"}
        [:> Input
         {:placeholder (or placeholder (str "Выберите ID заказа"))
          :allowClear true
          }
         ]
        ]
       
       [:> FormItem
        {:name "track_number_filter"
         :label "Трек-номер"}
        [:> Input
         {:placeholder (or placeholder (str "Выберите трек-номер"))
          :allowClear true
          }
         ]
        ]
       ]
      )
    )
  )