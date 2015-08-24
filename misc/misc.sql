/*select all id_bytes dependency of a message (with self)*/
SELECT  flat.any_reference_id_bytes AS ID_BYTES
FROM   
(
SELECT ID_BYTES FROM ABSTRACT_MESSAGE 
WHERE MESSAGE_TYPE='PLAIN_TEXT' 
ORDER BY CREATE_DATE DESC
/*other condition goes here*/  
) 
AS all_msg 
LEFT JOIN   FLAT_REFERENCE as flat  
ON all_msg.ID_BYTES = flat.ID_BYTES


