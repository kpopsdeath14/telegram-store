(ns jemo-murge-admin-frontend.pages.payment-methods.modal-help
  (:require
   ["antd" :as antd]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]])
  )

(defn modal_help []
  (let [Modal antd/Modal
        visible? (reagent/cursor app-state [:payment_methods_help_modal_open?])
        help-type (reagent/cursor app-state [:payment_methods_help_type])
        help-content {"page"     {:title "Способы оплаты"
                                  :description "на этой странице перечислены все доступные способы оплаты. каждый способ подключается по-разному, подробнее по клику на каждый способ"}
                      "youkassa" {:title "Юкасса"
                                  :description "автоматический прием оплаты через систему 'Юкасса'. пользователь платит сразу при оформления заказа"}
                      "default"  {:title "Оплата после оформления"
                                  :description "при этом способе оплаты покупатель не платит при оформлении заказа. заказ создается в статус 'оформлен' и вы сами связываетесь с покупателем после заказа и принимаете оплату переводом/ссылку и тп. и потом вручную переводите заказ в статус 'оплачено'"}}]
    (fn []
      (let [current-help (get help-content @help-type {:title "" :description ""})]
        [:> Modal
         {:title (as-element [:div {:style {:font-size "18px" :font-weight 600}} (:title current-help)])
          :visible @visible?
          :closable true
          :onCancel #(swap! app-state assoc :payment_methods_help_modal_open? false)
          :footer nil
          :width 600}
         [:div {:style {:white-space "pre-wrap" :line-height "1.6" :font-size "14px"}}
          (:description current-help)]]))))
