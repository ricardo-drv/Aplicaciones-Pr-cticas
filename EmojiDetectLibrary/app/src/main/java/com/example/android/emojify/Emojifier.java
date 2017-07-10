package com.example.android.emojify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import timber.log.Timber;

/**
 * Created by Gato on 05/07/2017.
 */

public class Emojifier {
    public static String TAG = "Emojifier";

    private static final double SMILING_PROB_LIMITE = 0.15;
    private static final double EYE_OPEN_PROB_LIMITE = 0.5;
    private static final float EMOJI_SCALE_FACTOR = .9f;


    public static Bitmap detectFacesAndOverlayEmoji(Context context, Bitmap picture){
        FaceDetector detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        Frame frame = new Frame.Builder().setBitmap(picture).build();
        SparseArray<Face> faces = detector.detect(frame);

        Timber.d("número de caras detectadas: " + faces.size());
        Emoji emojiMatch;
        //bitmap con el resultado final
        Bitmap resultBitmap = picture;
        if (faces.size() == 0){
            Toast.makeText(context,"Caras no detectadas",Toast.LENGTH_SHORT).show();
        }else{

            for (int i = 0; i < faces.size() ; i++) {
                int key = faces.keyAt(i);
                // get the object by the key.
                Face face = faces.get(key);

                Bitmap emojiBitmap;
                emojiMatch = whichEmoji(face);
                //determina la expresión del emoji
                switch (emojiMatch){
                    case SMILE:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.smile);
                        break;
                    case CLICK_DERECHO:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.leftwink);
                        break;
                    case CLICK_IZQUIERDO:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.rightwink);
                        break;
                    case CLOSE_SMILE:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.closed_smile);
                        break;
                    case SERIO:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.frown);
                        break;
                    case SERIO_CLICK_DE:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.leftwinkfrown);
                        break;
                    case SERIO_CLICK_IZ:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.rightwinkfrown);
                        break;
                    case SERIO_CLOSE_SMILE:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.closed_frown);
                        break;
                    default:
                        emojiBitmap = null;
                        Toast.makeText(context,"expresión no detectada",Toast.LENGTH_SHORT).show();
                        break;
                }

                resultBitmap = addBitmapToFace(resultBitmap,emojiBitmap,face);
            }
        }


        detector.release();
        return resultBitmap;
    }

    private static Emoji whichEmoji(Face face){
        Timber.d("whichEmoji: smilingProb = " + face.getIsSmilingProbability());
        Timber.d( "whichEmoji: ojo izquierdo prob = " + face.getIsLeftEyeOpenProbability());
        Timber.d( "whichEmoji: ojo derecho prob = " + face.getIsRightEyeOpenProbability());

        boolean sonrisa = face.getIsSmilingProbability() > SMILING_PROB_LIMITE;
        boolean iz_cerrado = face.getIsLeftEyeOpenProbability() < EYE_OPEN_PROB_LIMITE;
        boolean de_cerrado = face.getIsRightEyeOpenProbability() < EYE_OPEN_PROB_LIMITE;

        Emoji emoji;
        if (sonrisa){
            if (iz_cerrado && !de_cerrado){
                emoji = Emoji.CLICK_IZQUIERDO;
            } else if (!iz_cerrado && de_cerrado){
                emoji = Emoji.CLICK_DERECHO;
            } else if (iz_cerrado && de_cerrado){
                emoji = Emoji.CLOSE_SMILE;
            }
            else {
                emoji = Emoji.SMILE;
            }
        }else {
            if (iz_cerrado && !de_cerrado){
                emoji = Emoji.SERIO_CLICK_IZ;
            } else if (!iz_cerrado && de_cerrado){
                emoji = Emoji.SERIO_CLICK_DE;
            }else if (iz_cerrado && de_cerrado){
                emoji = Emoji.SERIO_CLOSE_SMILE;
            } else {
                emoji = Emoji.SERIO;
            }

        }
        Timber.d("expresión selecionada: " + emoji.name());

        return emoji;
    }

    /**
     * Combines the original picture with the emoji bitmaps
     *
     * @param backgroundBitmap The original picture
     * @param emojiBitmap      The chosen emoji
     * @param face             The detected face
     * @return The final bitmap, including the emojis over the faces
     */
    private static Bitmap addBitmapToFace(Bitmap backgroundBitmap, Bitmap emojiBitmap, Face face) {

        // Initialize the results bitmap to be a mutable copy of the original image
        Bitmap resultBitmap = Bitmap.createBitmap(backgroundBitmap.getWidth(),
                backgroundBitmap.getHeight(), backgroundBitmap.getConfig());

        // Scale the emoji so it looks better on the face
        float scaleFactor = EMOJI_SCALE_FACTOR;

        // Determine the size of the emoji to match the width of the face and preserve aspect ratio
        int newEmojiWidth = (int) (face.getWidth() * scaleFactor);
        int newEmojiHeight = (int) (emojiBitmap.getHeight() *
                newEmojiWidth / emojiBitmap.getWidth() * scaleFactor);


        // Scale the emoji
        emojiBitmap = Bitmap.createScaledBitmap(emojiBitmap, newEmojiWidth, newEmojiHeight, false);

        // Determine the emoji position so it best lines up with the face
        float emojiPositionX =
                (face.getPosition().x + face.getWidth() / 2) - emojiBitmap.getWidth() / 2;
        float emojiPositionY =
                (face.getPosition().y + face.getHeight() / 2) - emojiBitmap.getHeight() / 3;

        // Create the canvas and draw the bitmaps to it
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        canvas.drawBitmap(emojiBitmap, emojiPositionX, emojiPositionY, null);

        return resultBitmap;
    }

    private enum Emoji {
        SMILE,
        SERIO,
        CLICK_DERECHO,
        CLICK_IZQUIERDO,
        CLOSE_SMILE,
        SERIO_CLICK_DE,
        SERIO_CLICK_IZ,
        SERIO_CLOSE_SMILE
        
    }
}//fin clase
