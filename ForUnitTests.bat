java -Dmusic.volume=1 -jar target\BDD6502-1.0.9-SNAPSHOT-jar-with-dependencies.jar --exportmod "C:\Users\Martin Piper\Downloads\asikwp_-_twistmachine.mod" "target/exportedMusic" 1 1

pushd ..\C64\VideoHardware\assets
call convert4.bat

java -jar ..\..\..\ImageToBitplane\target\imagetobitplane-1.0-SNAPSHOT-jar-with-dependencies.jar --palettesize 16 --rgbshift 4 4 4 --newpalettes --loadpalette ../tmp/TurricanPaletteData.bin --chars --numbitplanes 4 --image "RPG status 512.png" --tilewh 8 8 --fitpalettes --nostacking --outputplanes ../tmp/TurricanStatus_plane512 --outputscrcol ../tmp/TurricanStatus_map512.bin --convertwritepass

rem Turrican Scaled
java -jar ..\..\..\ImageToBitplane\target\imagetobitplane-1.0-SNAPSHOT-jar-with-dependencies.jar --rgbshift 4 4 4 --newpalettes --palettesize 16 --forcergb 255 0 255 --image "Turrican/player.png" --tilewh 32 32 --imagequantize 16 --palettequantize 16 --nostacking --outputscaled ../tmp/TurricanScaledSprites --outputsprites ../tmp/TurricanScaledSpritesSheet.txt --outputpalettes ../tmp/TurricanScaledPaletteData.bin --convertwritepass

popd


pushd ..\ImageToBitplane
rem oldbridge char screen with rgbfactor 512
java -jar target\imagetobitplane-1.0-SNAPSHOT-jar-with-dependencies.jar --chars --rgbshift 4 4 4 --rgbfactor 255 196 112 10 --rgbfactor 255 255 214 50 --rgbfactor 236 98 96 50 --newpalettes --forcergb 0 0 0 --paletteoffset 0 --palettesize 16 --startxy 0 0 --image "src/test/resources/oldbridge cropped_chars 512.bmp" --tilewh 8 8 --imagequantize 16 --nowritepass --palettequantize 16 --image "src/test/resources/oldbridge cropped_chars 512.bmp" --tilewh 8 8 --fitpalettes --outputplanes target/chars512_plane --outputscrcol target/chars512_scr.bin --outputpalettes target/chars512_paletteData.bin --nostacking --numbitplanes 4 --convertwritepass

rem oldbridge char screen with rgbfactor 1024
java -jar target\imagetobitplane-1.0-SNAPSHOT-jar-with-dependencies.jar --splitmaps --chars --rgbshift 4 4 4 --rgbfactor 255 196 112 10 --rgbfactor 255 255 214 50 --rgbfactor 236 98 96 50 --newpalettes --forcergb 0 0 0 --paletteoffset 0 --palettesize 16 --startxy 0 0 --image "src/test/resources/oldbridge cropped_chars 512.bmp" --tilewh 8 8 --imagequantize 16 --nowritepass --palettequantize 16 --image "src/test/resources/oldbridge cropped_chars 1024.bmp" --tilewh 8 8 --fitpalettes --outputplanes target/chars1024_plane --outputscrcol target/chars1024_scr.bin --outputpalettes target/chars1024_paletteData.bin --nostacking --numbitplanes 4 --convertwritepass


rem map_9 - Copy - chars.png char screen
java -jar target\imagetobitplane-1.0-SNAPSHOT-jar-with-dependencies.jar --rgbshift 4 4 4 --newpalettes --forcergb 0 0 0 --paletteoffset 0 --palettesize 8 --startxy 0 0 --image "src/test/resources/map_9 - Copy - chars.png" --tilewh 8 8 --imagequantize 8 --nowritepass --palettequantize 16 --image "src/test/resources/map_9 - Copy - chars.png" --tilewh 8 8 --fitpalettes --outputplanes target/chars_plane --outputscrcol target/chars_scr.bin --outputpalettes target/chars_paletteData.bin --nostacking --numbitplanes 3 --convertwritepass 


rem buildings concat
java -jar target\imagetobitplane-1.0-SNAPSHOT-jar-with-dependencies.jar --concat "C:\Work\C64\VideoHardware\assets\building top 1.png" "C:\Work\C64\VideoHardware\assets\building top 2.png" target/testconcat2.png --concat "C:\Work\C64\VideoHardware\assets\building top 3.png" "C:\Work\C64\VideoHardware\assets\building top 4.png" target/testconcat3.png --concat target/testconcat2.png target/testconcat3.png target/testconcat4.png

rem Building Scaled
java -jar target\imagetobitplane-1.0-SNAPSHOT-jar-with-dependencies.jar --rgbshift 4 4 4 --newpalettes --palettesize 16 --forcergb 255 0 255 --image target\testconcat4.png --imagequantize 16 --tilewh 32 32 --imagequantize 16 --nostacking --outputscaled target\testconcat4ScaledSprites --outputsprites target\testconcat4ScaledSpritesSheet.txt --outputpalettes target\testconcat4PaletteData.bin --convertwritepass

popd
