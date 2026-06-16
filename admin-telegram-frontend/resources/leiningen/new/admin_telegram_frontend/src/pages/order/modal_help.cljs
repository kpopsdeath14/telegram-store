(ns {{name}}.pages.order.modal-help
  (:require
   ["antd" :as antd]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]])
  )

(defn modal_help []
  (let [Modal antd/Modal
        visible? (reagent/cursor app-state [:order_help_modal_open?])
        help-type (reagent/cursor app-state [:order_help_type])
        help-content {"order_id"     {:title "Номер заказа"
                                      :description "после '#' следует id заказа"}
                      "write_client" {:title "Написать клиенту"
                                      :description "переключение на чат с покупателем"}
                      "status"       {:title "Текущий статус"
                                      :description "по нажатию можно установить новый статус"}
                      "track_number" {:title "Трек-номер"
                                      :description "введите трек-номер и нажмите «Сохранить трек-номер» — покупатель автоматически получит уведомление с номером для отслеживания"}}]
    (fn []
      (let [current-help (get help-content @help-type {:title "" :description ""})]
        [:> Modal
         {:title (as-element [:div {:style {:font-size "18px" :font-weight 600}} (:title current-help)])
          :visible @visible?
          :closable true
          :onCancel #(swap! app-state assoc :order_help_modal_open? false)
          :footer nil
          :width 600}
         [:div {:style {:white-space "pre-wrap" :line-height "1.6" :font-size "14px"}}
          (:description current-help)]]))))
