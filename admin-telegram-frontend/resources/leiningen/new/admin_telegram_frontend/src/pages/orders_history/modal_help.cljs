(ns {{name}}.pages.orders-history.modal-help
  (:require
   ["antd" :as antd]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]])
  )

(defn modal_help []
  (let [Modal antd/Modal
        visible? (reagent/cursor app-state [:orders_history_help_modal_open?])
        help-type (reagent/cursor app-state [:orders_history_help_type])
        help-content {"page" {:title "Заказы"
                               :description "на этой странице можно смотреть какие есть заказы в магазине. сначала идут фильтры для поиска заказов, потом сами заказы. в каждый заказ можно перейти, кликнув по нему"}}]
    (fn []
      (let [current-help (get help-content @help-type {:title "" :description ""})]
        [:> Modal
         {:title (as-element [:div {:style {:font-size "18px" :font-weight 600}} (:title current-help)])
          :visible @visible?
          :closable true
          :onCancel #(swap! app-state assoc :orders_history_help_modal_open? false)
          :footer nil
          :width 600}
         [:div {:style {:white-space "pre-wrap" :line-height "1.6" :font-size "14px"}}
          (:description current-help)]]))))
